package com.example.lifet.feature.fitness

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.fitness.ObserveFitnessOverview
import com.example.lifet.core.model.fitness.FitnessOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
internal class FitnessViewModel
    @Inject
    constructor(
        private val observeOverview: ObserveFitnessOverview,
        private val writer: FitnessWriter,
        private val clock: Clock,
    ) : ViewModel() {
        private val retry = MutableStateFlow(0)
        private val _editor = MutableStateFlow<FitnessEditor?>(null)
        private val _effects = Channel<FitnessEffect>(Channel.BUFFERED)

        val editor: StateFlow<FitnessEditor?> = _editor
        val effects: Flow<FitnessEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<FitnessUiState> =
            retry
                .flatMapLatest {
                    observeOverview()
                        .map<FitnessOverview, FitnessUiState> { overview ->
                            if (overview.hasAnyRecord) {
                                FitnessUiState.Success(
                                    overview,
                                )
                            } else {
                                FitnessUiState.Empty(overview)
                            }
                        }.catch { emit(FitnessUiState.Error(R.string.fitness_error_load)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FitnessUiState.Loading)

        fun onAction(action: FitnessAction) {
            when (action) {
                FitnessAction.Retry -> retry.value += 1
                is FitnessAction.Open -> _editor.value = action.editor
                is FitnessAction.OpenWorkout -> openWorkout(action)
                is FitnessAction.Save -> save(action.form)
                is FitnessAction.Delete -> delete(action.item)
                is FitnessAction.Undo -> undo(action.item)
            }
        }

        /** Estampa "hoy" y "ahora" con el reloj inyectado: la UI no conoce el reloj. */
        private fun openWorkout(action: FitnessAction.OpenWorkout) {
            val now = LocalTime.now(clock)
            _editor.value =
                FitnessEditor.EditWorkout(
                    existing = action.existing,
                    seed = action.seed,
                    newId = UUID.randomUUID().toString(),
                    today = LocalDate.now(clock),
                    zone = clock.zone,
                    nowMinute = now.hour * MINUTES_IN_HOUR + now.minute,
                )
        }

        private fun save(form: FitnessForm) {
            viewModelScope.launch {
                when (val result = writer.save(form)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(FitnessEffect.Saved)
                    }

                    is AppResult.Failure -> {
                        _effects.send(FitnessEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun delete(item: FitnessItem) {
            viewModelScope.launch {
                when (val result = writer.delete(item)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(FitnessEffect.Deleted(item.deletedMessageRes(), item))
                    }

                    is AppResult.Failure -> {
                        _effects.send(FitnessEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun undo(item: FitnessItem) {
            viewModelScope.launch {
                if (writer.restore(item) is AppResult.Failure) {
                    _effects.send(FitnessEffect.Message(R.string.fitness_error_save))
                }
            }
        }

        private fun AppError.messageRes(): Int =
            when (this) {
                is AppError.Validation -> R.string.fitness_error_invalid
                else -> R.string.fitness_error_save
            }

        @StringRes
        private fun FitnessItem.deletedMessageRes(): Int =
            when (this) {
                is FitnessItem.Workout -> R.string.fitness_deleted_workout
                is FitnessItem.Weight -> R.string.fitness_deleted_weight
                is FitnessItem.Steps -> R.string.fitness_deleted_steps
                is FitnessItem.Template -> R.string.fitness_deleted_template
            }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MINUTES_IN_HOUR = 60
        }
    }
