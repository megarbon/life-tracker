package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.fitness.ExerciseProgress
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.quantity
import com.example.lifet.feature.fitness.rememberFitnessFormatters

/** Lista de ejercicios de fuerza con su última sesión y mejor marca; tocar uno abre su progresión. */
@Composable
internal fun ExercisesTab(
    overview: FitnessOverview,
    contentPadding: PaddingValues,
    onOpen: (ExerciseProgress) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberFitnessFormatters()
    // Más recientes primero: lo que se entrena ahora es lo que se quiere consultar.
    val exercises = overview.history.exercises.sortedByDescending { it.lastDate }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        if (exercises.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.fitness_exercises_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(exercises, key = { it.key }, contentType = { "exercise" }) { exercise ->
            ExerciseRow(exercise, formatters) { onOpen(exercise) }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseProgress,
    formatters: FitnessFormatters,
    onClick: () -> Unit,
) {
    val sessions = quantity(R.plurals.fitness_exercise_sessions_count, exercise.sessions.size)
    val lastDate = exercise.lastDate?.let(formatters::date).orEmpty()
    val meta = stringResource(R.string.fitness_exercise_meta, sessions, lastDate)
    val best =
        if (exercise.isBodyweightOnly) {
            quantity(R.plurals.fitness_exercise_best_reps, exercise.bestReps ?: 0)
        } else {
            stringResource(R.string.fitness_exercise_best_load, formatters.kilos(exercise.bestOneRepMaxGrams ?: 0L))
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = "${exercise.name}. $best. $meta"
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = LifeTrackerTheme.areaColors.fitness)
        Column(Modifier.weight(1f)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(best, style = MaterialTheme.typography.bodyMedium)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
