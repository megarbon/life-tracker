package com.example.lifet.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.mood.DeleteMoodEntry
import com.example.lifet.core.domain.mood.GetCustomMoodTags
import com.example.lifet.core.domain.mood.MoodDraft
import com.example.lifet.core.domain.mood.ObserveMoodEntries
import com.example.lifet.core.domain.mood.ObserveMoodSeries
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.domain.mood.SaveMoodEntry
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val MIN_INTENSITY = 0.1f
private const val SCALE_SPAN = 4.0

/** Pasa un ánimo medio de 1 a 5 a la intensidad 0,1 a 1,0 que usa el calendario (0 se reserva para "sin dato"). */
internal fun moodIntensity(average: Double): Float {
    val fraction = ((average - MoodLevel.MIN) / SCALE_SPAN).toFloat()
    return (MIN_INTENSITY + (1f - MIN_INTENSITY) * fraction).coerceIn(MIN_INTENSITY, 1f)
}

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
@HiltViewModel
internal class MoodViewModel
    @Inject
    constructor(
        observeEntries: ObserveMoodEntries,
        observeTodaySummary: ObserveMoodTodaySummary,
        observeSeries: ObserveMoodSeries,
        private val saveEntry: SaveMoodEntry,
        private val deleteEntry: DeleteMoodEntry,
        private val getCustomTags: GetCustomMoodTags,
        getToday: GetTodayUseCase,
    ) : ViewModel() {
        private val today: LocalDate = getToday()
        private val range = MutableStateFlow(DateRange.containing(DateRangeUnit.MONTH, today))
        private val calendarYear = MutableStateFlow(today.year)
        private val supportDismissed = MutableStateFlow(false)
        private val retryTrigger = MutableStateFlow(0)
        private val _overlay = MutableStateFlow<MoodOverlay>(MoodOverlay.None)
        private val effectChannel = Channel<MoodEffect>(Channel.BUFFERED)
        private var undoAction: (suspend () -> Unit)? = null

        val overlay: StateFlow<MoodOverlay> = _overlay.asStateFlow()
        val effects: Flow<MoodEffect> = effectChannel.receiveAsFlow()

        private val dataState: Flow<MoodUiState> =
            combine(
                observeEntries(),
                observeTodaySummary(),
                range.flatMapLatest { observeSeries(it) },
                calendarYear,
                supportDismissed,
            ) { entries, summary, series, year, dismissed ->
                if (entries.isEmpty()) {
                    MoodUiState.Empty
                } else {
                    MoodUiState.Success(
                        today = today,
                        summary = summary,
                        series = series,
                        journal = entries.toJournal(),
                        calendar = buildCalendar(entries, year),
                        supportVisible = summary.showSupport && !dismissed,
                    )
                }
            }

        val uiState: StateFlow<MoodUiState> =
            retryTrigger
                .flatMapLatest {
                    dataState
                        .onStart { emit(MoodUiState.Loading) }
                        .catch { emit(MoodUiState.Error(R.string.mood_error_load)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MoodUiState.Loading)

        fun onAction(action: MoodAction) {
            when (action) {
                is MoodAction.QuickCheckIn -> quickCheckIn(action.level)
                MoodAction.OpenNewForm -> openForm(MoodFormState())
                is MoodAction.OpenEdit -> openForm(MoodFormState.from(action.entry))
                is MoodAction.OpenDetail -> _overlay.value = MoodOverlay.Detail(action.entry)
                MoodAction.DismissOverlay -> _overlay.value = MoodOverlay.None
                is MoodAction.UpdateForm -> updateForm(action.transform)
                MoodAction.SaveForm -> saveForm()
                is MoodAction.Delete -> delete(action.entry)
                MoodAction.Undo -> undo()
                is MoodAction.ChangeRange -> range.value = action.range
                is MoodAction.ChangeYear -> calendarYear.value = action.year
                MoodAction.DismissSupport -> supportDismissed.value = true
                MoodAction.Retry -> retryTrigger.update { it + 1 }
            }
        }

        private fun quickCheckIn(level: MoodLevel) {
            viewModelScope.launch {
                when (val result = saveEntry(MoodDraft(level = level))) {
                    is AppResult.Success -> {
                        undoAction = { deleteEntry(result.value) }
                        effectChannel.send(MoodEffect.Undoable(R.string.mood_quick_saved))
                    }

                    is AppResult.Failure -> {
                        effectChannel.send(MoodEffect.Message(R.string.mood_error_save))
                    }
                }
            }
        }

        private fun updateForm(transform: (MoodFormState) -> MoodFormState) {
            _overlay.update { current ->
                if (current is MoodOverlay.Form && !current.state.saving) {
                    MoodOverlay.Form(transform(current.state))
                } else {
                    current
                }
            }
        }

        private fun saveForm() {
            val form = (_overlay.value as? MoodOverlay.Form)?.state ?: return
            val draft = form.toDraft()
            if (draft == null || form.saving) return
            _overlay.value = MoodOverlay.Form(form.copy(saving = true))
            viewModelScope.launch {
                when (saveEntry(draft)) {
                    is AppResult.Success -> {
                        _overlay.value = MoodOverlay.None
                        val message = if (form.isEditing) R.string.mood_updated else R.string.mood_saved
                        effectChannel.send(MoodEffect.Message(message, success = true))
                    }

                    is AppResult.Failure -> {
                        _overlay.value = MoodOverlay.Form(form.copy(saving = false))
                        effectChannel.send(MoodEffect.Message(R.string.mood_error_save))
                    }
                }
            }
        }

        private fun delete(entry: MoodEntry) {
            _overlay.value = MoodOverlay.None
            viewModelScope.launch {
                when (deleteEntry(entry)) {
                    is AppResult.Success -> {
                        undoAction = { saveEntry(entry.toDraft()) }
                        effectChannel.send(MoodEffect.Undoable(R.string.mood_deleted))
                    }

                    is AppResult.Failure -> {
                        effectChannel.send(MoodEffect.Message(R.string.mood_error_delete))
                    }
                }
            }
        }

        private fun undo() {
            val action = undoAction ?: return
            undoAction = null
            viewModelScope.launch { action() }
        }

        private fun openForm(initial: MoodFormState) {
            _overlay.value = MoodOverlay.Form(initial)
            viewModelScope.launch {
                val history = (getCustomTags() as? AppResult.Success)?.value ?: return@launch
                updateForm {
                    it.copy(suggestedEmotions = history.emotions, suggestedFactors = history.factors)
                }
            }
        }

        private fun List<MoodEntry>.toJournal() =
            groupBy { it.localDate }
                .map { (date, dayEntries) ->
                    MoodDayGroup(
                        date = date,
                        average = dayEntries.map { it.level.value }.average(),
                        entries = dayEntries.sortedByDescending { it.instant }.toImmutableList(),
                    )
                }.sortedByDescending { it.date }
                .toImmutableList()

        private fun buildCalendar(
            entries: List<MoodEntry>,
            year: Int,
        ): MoodCalendar {
            val yearRange = DateRange.containing(DateRangeUnit.YEAR, LocalDate.of(year, 1, 1))
            val daily = MoodStats.dailyAverages(entries).filterKeys { it in yearRange }
            val firstYear = entries.minOf { it.localDate.year }
            return MoodCalendar(
                range = yearRange,
                intensity = daily.mapValues { moodIntensity(it.value) },
                loggedDays = daily.size,
                average = daily.values.takeIf { it.isNotEmpty() }?.average(),
                hasPreviousYear = year > firstYear,
                hasNextYear = year < today.year,
            )
        }
    }
