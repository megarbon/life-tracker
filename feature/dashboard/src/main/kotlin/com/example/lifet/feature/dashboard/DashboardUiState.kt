package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.finance.FinanceTodaySummary
import com.example.lifet.core.model.fitness.FitnessTodaySummary
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.model.nutrition.NutritionTodaySummary
import com.example.lifet.core.model.quit.QuitTodaySummary
import java.time.LocalDate

/** Estado de la pantalla "Hoy". Cada área aporta su tarjeta a medida que se integra. */
internal sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    @Immutable
    data class Success(
        val today: LocalDate,
        val mood: MoodTodaySummary,
        val finance: FinanceTodaySummary,
        val nutrition: NutritionTodaySummary,
        val fitness: FitnessTodaySummary,
        val quit: QuitTodaySummary,
    ) : DashboardUiState
}
