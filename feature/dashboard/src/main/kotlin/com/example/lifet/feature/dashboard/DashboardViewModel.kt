package com.example.lifet.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.finance.ObserveFinanceTodaySummary
import com.example.lifet.core.domain.fitness.ObserveFitnessTodaySummary
import com.example.lifet.core.domain.mood.MoodDraft
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.domain.mood.SaveMoodEntry
import com.example.lifet.core.domain.nutrition.AddWater
import com.example.lifet.core.domain.nutrition.ObserveNutritionTodaySummary
import com.example.lifet.core.domain.quit.ObserveQuitTodaySummary
import com.example.lifet.core.model.finance.FinanceTodaySummary
import com.example.lifet.core.model.fitness.FitnessTodaySummary
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.model.nutrition.NutritionTodaySummary
import com.example.lifet.core.model.quit.QuitTodaySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mensajes puntuales para mostrar en un snackbar. */
internal enum class DashboardMessage { MOOD_SAVED, MOOD_FAILED, WATER_SAVED, WATER_FAILED }

@HiltViewModel
internal class DashboardViewModel
    @Inject
    constructor(
        getToday: GetTodayUseCase,
        observeMood: ObserveMoodTodaySummary,
        observeFinance: ObserveFinanceTodaySummary,
        observeNutrition: ObserveNutritionTodaySummary,
        observeFitness: ObserveFitnessTodaySummary,
        observeQuit: ObserveQuitTodaySummary,
        private val saveMood: SaveMoodEntry,
        private val addWater: AddWater,
    ) : ViewModel() {
        private val today = getToday()

        val uiState: StateFlow<DashboardUiState> =
            combine(observeMood(), observeFinance(), observeNutrition(), observeFitness(), observeQuit()) { areas ->
                DashboardUiState.Success(
                    today = today,
                    mood = areas[MOOD] as MoodTodaySummary,
                    finance = areas[FINANCE] as FinanceTodaySummary,
                    nutrition = areas[NUTRITION] as NutritionTodaySummary,
                    fitness = areas[FITNESS] as FitnessTodaySummary,
                    quit = areas[QUIT] as QuitTodaySummary,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState.Loading)

        private val messageChannel = Channel<DashboardMessage>(Channel.BUFFERED)
        val messages: Flow<DashboardMessage> = messageChannel.receiveAsFlow()

        fun checkIn(level: MoodLevel) {
            viewModelScope.launch {
                val message =
                    when (saveMood(MoodDraft(level = level))) {
                        is AppResult.Success -> DashboardMessage.MOOD_SAVED
                        is AppResult.Failure -> DashboardMessage.MOOD_FAILED
                    }
                messageChannel.send(message)
            }
        }

        fun addGlassOfWater() {
            viewModelScope.launch {
                val message =
                    when (addWater(today)) {
                        is AppResult.Success -> DashboardMessage.WATER_SAVED
                        is AppResult.Failure -> DashboardMessage.WATER_FAILED
                    }
                messageChannel.send(message)
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MOOD = 0
            const val FINANCE = 1
            const val NUTRITION = 2
            const val FITNESS = 3
            const val QUIT = 4
        }
    }
