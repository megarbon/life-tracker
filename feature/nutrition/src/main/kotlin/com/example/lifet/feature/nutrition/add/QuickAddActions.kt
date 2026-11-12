package com.example.lifet.feature.nutrition.add

import com.example.lifet.core.model.nutrition.MealType

/** Acciones que la hoja rápida delega en el Diario. */
internal class QuickAddActions(
    val onDismiss: () -> Unit,
    val onSearch: (MealType) -> Unit,
    val onRepeatYesterday: () -> Unit,
    val onAddWater: () -> Unit,
    val onEntriesAdded: (List<String>) -> Unit,
)
