package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

/**
 * Grupo de chips con selección simple ([LTChipGroup] con `onSelect`) o múltiple ([LTMultiChipGroup]).
 * Se reparten en varias líneas para que la fuente grande no recorte etiquetas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun <T> LTChipGroup(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/** Variante de selección múltiple: cada toque alterna la opción en [selected]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun <T> LTMultiChipGroup(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@LTPreviews
@Composable
private fun LTChipGroupPreview() {
    LifeTrackerTheme {
        LTChipGroup(
            options = listOf("Desayuno", "Comida", "Cena", "Snack"),
            selected = "Comida",
            label = { it },
            onSelect = {},
        )
    }
}
