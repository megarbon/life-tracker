package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/** Estado de la pantalla "Hoy". Las tarjetas por área se añadirán con cada feature. */
internal sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    @Immutable
    data class Success(
        val today: LocalDate,
    ) : DashboardUiState
}
