package com.example.lifet.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.ObserveToday
import com.example.lifet.core.domain.finance.ObserveFinanceTodaySummary
import com.example.lifet.core.domain.fitness.ObserveFitnessTodaySummary
import com.example.lifet.core.domain.mood.DeleteMoodEntry
import com.example.lifet.core.domain.mood.MoodDraft
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.domain.mood.SaveMoodEntry
import com.example.lifet.core.domain.nutrition.AddWater
import com.example.lifet.core.domain.nutrition.ManageDiaryEntries
import com.example.lifet.core.domain.nutrition.ObserveNutritionTodaySummary
import com.example.lifet.core.domain.quit.ObserveQuitTodaySummary
import com.example.lifet.core.domain.work.ObserveWorkTodaySummary
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.nutrition.WaterEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Mensajes puntuales para mostrar en un snackbar; los que se pueden deshacer llevan lo necesario para revertirlos. */
internal sealed interface DashboardMessage {
    data class MoodSaved(
        val entryId: Long,
    ) : DashboardMessage

    data object MoodFailed : DashboardMessage

    data class WaterSaved(
        val entry: WaterEntry,
    ) : DashboardMessage

    data object WaterFailed : DashboardMessage
}

/** Agrupa los casos de uso de lectura para no pasar de los parámetros que detekt tolera en el constructor. */
internal class DashboardObservers
    @Inject
    constructor(
        val today: ObserveToday,
        val mood: ObserveMoodTodaySummary,
        val finance: ObserveFinanceTodaySummary,
        val nutrition: ObserveNutritionTodaySummary,
        val fitness: ObserveFitnessTodaySummary,
        val quit: ObserveQuitTodaySummary,
        val work: ObserveWorkTodaySummary,
    )

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class DashboardViewModel
    @Inject
    constructor(
        observers: DashboardObservers,
        private val saveMood: SaveMoodEntry,
        private val deleteMood: DeleteMoodEntry,
        private val addWater: AddWater,
        private val diary: ManageDiaryEntries,
        val clock: Clock,
    ) : ViewModel() {
        private val todayFlow = observers.today()

        // Al cambiar el día se vuelven a suscribir todas las áreas, así sus "hoy" internos se recalculan.
        val uiState: StateFlow<DashboardUiState> =
            todayFlow
                .flatMapLatest { today ->
                    val first =
                        combine(
                            observers.mood().asArea(),
                            observers.finance().asArea(),
                            observers.nutrition().asArea(),
                        ) { mood, finance, nutrition -> Triple(mood, finance, nutrition) }
                    val second =
                        combine(
                            observers.fitness().asArea(),
                            observers.quit().asArea(),
                            observers.work().asArea(),
                        ) { fitness, quit, work -> Triple(fitness, quit, work) }
                    combine(first, second) { (mood, finance, nutrition), (fitness, quit, work) ->
                        DashboardUiState.Success(today, mood, finance, nutrition, fitness, quit, work)
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState.Loading)

        private val messageChannel = Channel<DashboardMessage>(Channel.BUFFERED)
        val messages: Flow<DashboardMessage> = messageChannel.receiveAsFlow()

        fun checkIn(level: MoodLevel) {
            viewModelScope.launch {
                val message =
                    when (val result = saveMood(MoodDraft(level = level))) {
                        is AppResult.Success -> DashboardMessage.MoodSaved(result.value)
                        is AppResult.Failure -> DashboardMessage.MoodFailed
                    }
                messageChannel.send(message)
            }
        }

        fun addGlassOfWater() {
            viewModelScope.launch {
                // El día se lee al pulsar, no al crear el ViewModel: pasada la medianoche no se guarda en ayer.
                val today = todayFlow.first()
                val message =
                    when (val result = addWater(today)) {
                        is AppResult.Success -> DashboardMessage.WaterSaved(result.value)
                        is AppResult.Failure -> DashboardMessage.WaterFailed
                    }
                messageChannel.send(message)
            }
        }

        /** Deshace la última acción rápida mostrada en el snackbar. */
        fun undo(message: DashboardMessage) {
            viewModelScope.launch {
                when (message) {
                    is DashboardMessage.MoodSaved -> deleteMood(message.entryId)
                    is DashboardMessage.WaterSaved -> diary.deleteWater(message.entry)
                    DashboardMessage.MoodFailed, DashboardMessage.WaterFailed -> Unit
                }
            }
        }

        /** Un fallo al leer un área la degrada a [AreaState.Unavailable] sin afectar a las demás. */
        private fun <T> Flow<T>.asArea(): Flow<AreaState<T>> =
            map<T, AreaState<T>> { AreaState.Ready(it) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(AreaState.Unavailable)
                }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
