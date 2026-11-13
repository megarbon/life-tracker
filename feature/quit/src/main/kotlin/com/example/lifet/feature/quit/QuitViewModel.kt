package com.example.lifet.feature.quit

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.quit.ObserveQuitOverview
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitOverview
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
internal class QuitViewModel
    @Inject
    constructor(
        private val observeOverview: ObserveQuitOverview,
        private val writer: QuitWriter,
        /** Reloj inyectado: la UI lo usa para el contador en vivo en lugar de leer la hora por su cuenta. */
        val clock: Clock,
    ) : ViewModel() {
        private val reload = MutableStateFlow(0)
        private val _editor = MutableStateFlow<QuitEditor?>(null)
        private val _effects = Channel<QuitEffect>(Channel.BUFFERED)

        val editor: StateFlow<QuitEditor?> = _editor
        val effects: Flow<QuitEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<QuitUiState> =
            reload
                .flatMapLatest {
                    observeOverview()
                        .map<QuitOverview, QuitUiState> { overview ->
                            if (overview.hasAnyRecord) QuitUiState.Success(overview) else QuitUiState.Empty
                        }.catch { emit(QuitUiState.Error(R.string.quit_error_load)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), QuitUiState.Loading)

        fun onAction(action: QuitAction) {
            when (action) {
                QuitAction.Retry, QuitAction.Refresh -> reload.value += 1
                is QuitAction.OpenEvent -> openEvent(action)
                is QuitAction.OpenHabit -> openHabit(action)
                is QuitAction.OpenSos -> _editor.value = QuitEditor.Sos(action.habitId)
                QuitAction.Dismiss -> _editor.value = null
                is QuitAction.Save -> save(action.form)
                is QuitAction.Delete -> delete(action.item)
                is QuitAction.Undo -> undo(action.item)
            }
        }

        /** Estampa "hoy" y "ahora" con el reloj inyectado: la UI no conoce el reloj. */
        private fun openEvent(action: QuitAction.OpenEvent) {
            val now = LocalTime.now(clock)
            _editor.value =
                QuitEditor.Event(
                    habitId = action.habitId,
                    existing = action.existing,
                    asConsumption = action.existing is QuitConsumption || action.asConsumption,
                    seedStrategy = action.seedStrategy,
                    newId = UUID.randomUUID().toString(),
                    now = clock.instant(),
                    today = LocalDate.now(clock),
                    zone = clock.zone,
                    nowMinute = now.hour * MINUTES_IN_HOUR + now.minute,
                )
        }

        private fun openHabit(action: QuitAction.OpenHabit) {
            val now = LocalTime.now(clock)
            _editor.value =
                QuitEditor.Habit(
                    existing = action.existing,
                    kind = action.existing?.kind ?: action.kind,
                    currency = action.currency,
                    newId = UUID.randomUUID().toString(),
                    now = clock.instant(),
                    today = LocalDate.now(clock),
                    zone = clock.zone,
                    nowMinute = now.hour * MINUTES_IN_HOUR + now.minute,
                )
        }

        private fun save(form: QuitForm) {
            viewModelScope.launch {
                when (val result = writer.save(form)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(QuitEffect.Saved(form.savedMessageRes()))
                    }

                    is AppResult.Failure -> {
                        _effects.send(QuitEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun delete(item: QuitItem) {
            viewModelScope.launch {
                when (val result = writer.delete(item)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(QuitEffect.Deleted(item.deletedMessageRes(), item))
                    }

                    is AppResult.Failure -> {
                        _effects.send(QuitEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun undo(item: QuitItem) {
            viewModelScope.launch {
                if (writer.restore(item) is AppResult.Failure) {
                    _effects.send(QuitEffect.Message(R.string.quit_error_save))
                }
            }
        }

        private fun AppError.messageRes(): Int =
            when (this) {
                is AppError.Validation -> R.string.quit_error_invalid
                else -> R.string.quit_error_save
            }

        @StringRes
        private fun QuitForm.savedMessageRes(): Int =
            when (this) {
                is QuitForm.Habit -> R.string.quit_saved_habit
                is QuitForm.Craving -> R.string.quit_saved_craving
                is QuitForm.Consumption -> R.string.quit_saved_consumption
            }

        @StringRes
        private fun QuitItem.deletedMessageRes(): Int =
            when (this) {
                is QuitItem.Habit -> R.string.quit_deleted_habit
                is QuitItem.Craving -> R.string.quit_deleted_craving
                is QuitItem.Consumption -> R.string.quit_deleted_consumption
            }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MINUTES_IN_HOUR = 60
        }
    }
