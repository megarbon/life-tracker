package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.icon
import com.example.lifet.feature.quit.labelRes

/** Nombre que se muestra de un hábito: el que escribió el usuario o, si lo dejó en blanco, su tipo. */
@Composable
internal fun QuitHabit.displayName(): String = name.ifBlank { stringResource(kind.labelRes) }

/**
 * Chips para cambiar de hábito y añadir otro. Sólo se muestra si hay más de uno o si se quiere añadir; con un único
 * hábito sigue ahí el chip de "añadir otro" para descubrirlo sin ocupar una pantalla propia.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HabitPicker(
    habits: List<QuitHabit>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        habits.forEach { habit ->
            val name = habit.displayName()
            val selected = habit.id == selectedId
            val description = if (selected) stringResource(R.string.quit_habit_selected, name) else name
            FilterChip(
                selected = selected,
                onClick = { onSelect(habit.id) },
                label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(habit.kind.icon, contentDescription = null) },
                modifier = Modifier.heightIn(min = MinTouchTarget).semantics { contentDescription = description },
            )
        }
        AssistChip(
            onClick = onAdd,
            label = { Text(stringResource(R.string.quit_habit_add_another), maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            modifier = Modifier.heightIn(min = MinTouchTarget),
        )
    }
}
