package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.finance.FinanceTodaySummary
import com.example.lifet.core.model.fitness.FitnessTodaySummary
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.model.nutrition.NutritionTodaySummary
import com.example.lifet.core.model.quit.QuitTodaySummary
import com.example.lifet.core.model.work.WorkTodaySummary
import java.time.LocalDate

/**
 * Resultado de leer una área para Hoy. Si una sola área falla (datos corruptos, un cálculo que no cabe) su tarjeta
 * se degrada a [Unavailable] y las demás siguen funcionando; un error en un área nunca debe tumbar la pantalla.
 */
internal sealed interface AreaState<out T> {
    @Immutable
    data class Ready<out T>(
        val value: T,
    ) : AreaState<T>

    data object Unavailable : AreaState<Nothing>
}

/** Estado de la pantalla "Hoy". Cada área aporta su tarjeta a medida que se integra. */
internal sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    @Immutable
    data class Success(
        val today: LocalDate,
        val mood: AreaState<MoodTodaySummary>,
        val finance: AreaState<FinanceTodaySummary>,
        val nutrition: AreaState<NutritionTodaySummary>,
        val fitness: AreaState<FitnessTodaySummary>,
        val quit: AreaState<QuitTodaySummary>,
        val work: AreaState<WorkTodaySummary>,
    ) : DashboardUiState
}
