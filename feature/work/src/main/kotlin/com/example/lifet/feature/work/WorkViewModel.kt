package com.example.lifet.feature.work

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.work.ManageFocusTimer
import com.example.lifet.core.domain.work.ManageWorkClock
import com.example.lifet.core.domain.work.ObserveWorkOverview
import com.example.lifet.core.domain.work.ObserveWorkWeek
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.work.WorkClockIssue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

private const val KEY_WEEK_OFFSET = "weekOffset"
private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
internal class WorkViewModel
    @Inject
    constructor(
        observeOverview: ObserveWorkOverview,
        observeWeek: ObserveWorkWeek,
        private val clockActions: ManageWorkClock,
        private val focusActions: ManageFocusTimer,
        private val writer: WorkWriter,
        val clock: Clock,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val retry = MutableStateFlow(0)
        private val _editor = MutableStateFlow<WorkEditor?>(null)
        private val _effects = Channel<WorkEffect>(Channel.BUFFERED)

        /** Semanas hacia atrás (0 = la actual) de la pestaña Semana. Sobrevive a que el proceso muera. */
        private val weekOffset = savedState.getStateFlow(KEY_WEEK_OFFSET, 0)

        val editor: StateFlow<WorkEditor?> = _editor
        val effects: Flow<WorkEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<WorkUiState> =
            combine(retry, weekOffset, ::Pair)
                .flatMapLatest { (_, offset) ->
                    val week = DateRange.containing(DateRangeUnit.WEEK, LocalDate.now(clock)).shift(offset.toLong())
                    combine(observeOverview(), observeWeek(week)) { overview, report ->
                        if (overview.hasAnyRecord) {
                            WorkUiState.Success(overview, report)
                        } else {
                            WorkUiState.Empty(overview, report)
                        }
                    }.map<WorkUiState, WorkUiState> { it }
                        .catch { emit(WorkUiState.Error(R.string.work_error_load)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkUiState.Loading)

        /**
         * Enruta cada acción. Sin `else`: una acción nueva que nadie maneje es un error de compilación, no un toque
         * que se pierde en silencio.
         */
        @Suppress("CyclomaticComplexMethod") // Tabla plana de una línea por acción: partirla sólo la dispersaría.
        fun onAction(action: WorkAction) {
            when (action) {
                WorkAction.Retry -> retry.value += 1
                is WorkAction.ClockIn -> perform { clockActions.clockIn(action.projectId) }
                WorkAction.StartBreak -> perform { clockActions.startBreak() }
                WorkAction.EndBreak -> perform { clockActions.endBreak() }
                WorkAction.ClockOut -> perform { clockActions.clockOut() }
                is WorkAction.StartFocus -> perform { action.startOn(focusActions) }
                WorkAction.StopFocus -> perform { focusActions.stop() }
                WorkAction.FinishFocus -> finishFocus()
                is WorkAction.MoveWeek -> moveWeek(action.delta)
                is WorkAction.Save -> save(action.form)
                is WorkAction.Delete -> delete(action.item)
                is WorkAction.Undo -> undo(action.item)
                WorkAction.Dismiss -> _editor.value = null
                is WorkAction.OpenEditor -> _editor.value = action.editor(clock)
            }
        }

        private fun moveWeek(delta: Int) {
            val next = if (delta == 0) 0 else (weekOffset.value + delta).coerceAtMost(0)
            savedState[KEY_WEEK_OFFSET] = next
        }

        /** Ejecuta una acción de fichaje o de foco y, si el dominio la rechaza, lo cuenta con un mensaje. */
        private fun <T> perform(block: suspend () -> AppResult<T>) {
            viewModelScope.launch {
                val result = block()
                if (result is AppResult.Failure) _effects.send(WorkEffect.Message(result.error.messageRes()))
            }
        }

        private fun finishFocus() {
            viewModelScope.launch {
                focusActions.finishIfDue()?.let { _effects.send(WorkEffect.FocusFinished(it.kind)) }
            }
        }

        private fun save(form: WorkForm) {
            viewModelScope.launch {
                when (val result = writer.save(form)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(WorkEffect.Saved)
                    }

                    is AppResult.Failure -> {
                        _effects.send(WorkEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun delete(item: WorkItem) {
            viewModelScope.launch {
                when (val result = writer.delete(item)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(WorkEffect.Deleted(item.deletedMessageRes(), item))
                    }

                    is AppResult.Failure -> {
                        _effects.send(WorkEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun undo(item: WorkItem) {
            viewModelScope.launch {
                if (writer.restore(item) is AppResult.Failure) {
                    _effects.send(WorkEffect.Message(R.string.work_error_save))
                }
            }
        }

        private fun AppError.messageRes(): Int =
            when (this) {
                is AppError.Validation -> validationMessageRes(reason)
                else -> R.string.work_error_save
            }

        @StringRes
        private fun WorkItem.deletedMessageRes(): Int =
            when (this) {
                is WorkItem.Shift -> R.string.work_deleted_shift
                is WorkItem.Project -> R.string.work_deleted_project
            }
    }
