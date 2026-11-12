package com.example.lifet.feature.nutrition.diary

import com.example.lifet.core.model.nutrition.MealType
import java.time.LocalDate

/** Navegación saliente del Diario; la feature no conoce a otras features, solo a sus propias pantallas. */
internal class DiaryNavigation(
    val onSearch: (MealType, LocalDate) -> Unit,
    val onTrends: () -> Unit,
    val onGoals: () -> Unit,
)
