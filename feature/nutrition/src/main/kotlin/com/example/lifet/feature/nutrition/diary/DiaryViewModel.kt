package com.example.lifet.feature.nutrition.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.nutrition.AddWater
import com.example.lifet.core.domain.nutrition.ManageDiaryEntries
import com.example.lifet.core.domain.nutrition.ObserveNutritionDiary
import com.example.lifet.core.domain.nutrition.RepeatYesterdayMeals
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.feature.nutrition.R
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
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class DiaryViewModel
    @Inject
    constructor(
        private val observeDiary: ObserveNutritionDiary,
        private val addWater: AddWater,
        private val repeatYesterday: RepeatYesterdayMeals,
        private val manageEntries: ManageDiaryEntries,
        getToday: GetTodayUseCase,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val today: LocalDate = getToday()
        private val retry = MutableStateFlow(0)

        val date: StateFlow<LocalDate> =
            savedStateHandle
                .getStateFlow(KEY_DAY, today.toEpochDay())
                .map { LocalDate.ofEpochDay(it) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, today)

        private val _effects = Channel<DiaryEffect>(Channel.BUFFERED)
        val effects: Flow<DiaryEffect> = _effects.receiveAsFlow()

        val uiState: StateFlow<DiaryUiState> =
            combine(date, retry) { day, _ -> day }
                .flatMapLatest { day ->
                    observeDiary(day)
                        .map { it.toUiState(today) }
                        .catch { emit(DiaryUiState.Error(R.string.nutrition_error_generic)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiaryUiState.Loading)

        fun onAction(action: DiaryAction) {
            when (action) {
                DiaryAction.PreviousDay -> moveDay(-1)

                // No hay futuro que registrar: la flecha "siguiente" se detiene en hoy.
                DiaryAction.NextDay -> if (date.value < today) moveDay(1)

                DiaryAction.GoToToday -> savedStateHandle[KEY_DAY] = today.toEpochDay()

                DiaryAction.Retry -> retry.value += 1

                DiaryAction.AddWater -> launchWater()

                DiaryAction.RemoveLastWater -> launchRemoveWater()

                DiaryAction.RepeatYesterday -> launchRepeat()

                is DiaryAction.Delete -> launchDelete(action.entry)

                is DiaryAction.Update -> launchUpdate(action)

                is DiaryAction.EntriesAdded -> offerUndoForAdded(action.ids)

                is DiaryAction.Undo -> launchUndo(action.action)
            }
        }

        private fun launchWater() {
            viewModelScope.launch {
                when (val result = addWater(date.value)) {
                    is AppResult.Success -> {
                        send(DiaryEffect.ShowUndo(R.string.nutrition_water_added, UndoAction.DeleteWater(result.value)))
                    }

                    is AppResult.Failure -> {
                        send(DiaryEffect.ShowError(R.string.nutrition_error_save))
                    }
                }
            }
        }

        private fun launchRemoveWater() {
            val last = (uiState.value as? DiaryUiState.Success)?.water?.lastOrNull() ?: return
            viewModelScope.launch {
                if (manageEntries.deleteWater(last) is AppResult.Success) {
                    send(DiaryEffect.ShowUndo(R.string.nutrition_water_removed, UndoAction.RestoreWater(last)))
                } else {
                    send(DiaryEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }

        private fun launchRepeat() {
            viewModelScope.launch {
                when (val result = repeatYesterday(date.value)) {
                    is AppResult.Failure -> {
                        send(DiaryEffect.ShowError(R.string.nutrition_error_save))
                    }

                    is AppResult.Success -> {
                        if (result.value.isEmpty()) {
                            send(DiaryEffect.ShowError(R.string.nutrition_nothing_yesterday))
                        } else {
                            val undo = UndoAction.DeleteMeals(result.value.map { it.id })
                            send(DiaryEffect.ShowUndo(R.string.nutrition_repeated, undo))
                        }
                    }
                }
            }
        }

        private fun launchDelete(entry: MealEntry) {
            viewModelScope.launch {
                if (manageEntries.deleteMeals(listOf(entry)) is AppResult.Success) {
                    val undo = UndoAction.RestoreMeals(listOf(entry))
                    send(DiaryEffect.ShowUndo(R.string.nutrition_entry_deleted, undo))
                } else {
                    send(DiaryEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }

        private fun launchUpdate(action: DiaryAction.Update) {
            viewModelScope.launch {
                val result = manageEntries.updateMeal(action.entry, action.grams, action.mealType)
                if (result is AppResult.Failure) send(DiaryEffect.ShowError(R.string.nutrition_error_save))
            }
        }

        private fun launchUndo(action: UndoAction) {
            viewModelScope.launch {
                if (manageEntries.undo(action) is AppResult.Failure) {
                    send(DiaryEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }

        private fun offerUndoForAdded(ids: List<String>) {
            if (ids.isEmpty()) return
            send(DiaryEffect.ShowUndo(R.string.nutrition_entry_added, UndoAction.DeleteMeals(ids)))
        }

        private fun moveDay(delta: Long) {
            savedStateHandle[KEY_DAY] = date.value.plusDays(delta).toEpochDay()
        }

        private fun send(effect: DiaryEffect) {
            _effects.trySend(effect)
        }

        private companion object {
            const val KEY_DAY = "day"
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

private suspend fun ManageDiaryEntries.undo(action: UndoAction): AppResult<Unit> =
    when (action) {
        is UndoAction.DeleteMeals -> deleteMealsById(action.ids)
        is UndoAction.RestoreMeals -> restoreMeals(action.entries)
        is UndoAction.DeleteWater -> deleteWater(action.entry)
        is UndoAction.RestoreWater -> restoreWater(action.entry)
    }
