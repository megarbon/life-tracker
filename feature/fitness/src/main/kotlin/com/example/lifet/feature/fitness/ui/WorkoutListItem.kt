package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.icon
import com.example.lifet.feature.fitness.labelRes
import com.example.lifet.feature.fitness.quantity

private val ICON_BADGE = 40.dp
private val BADGE_ICON = 18.dp

/**
 * Fila de un entreno: icono del tipo, título (o el tipo), un resumen y, a la derecha, la fecha. Si bate un récord
 * lleva una copa y la palabra "Récord": el logro nunca depende sólo del color.
 */
@Composable
internal fun WorkoutListItem(
    workout: Workout,
    formatters: FitnessFormatters,
    hasRecord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LifeTrackerTheme.areaColors.fitness
    val title = workout.title.ifBlank { stringResource(workout.type.labelRes) }
    val subtitle = workoutSummary(workout, formatters)
    val date = formatters.date(workout.startDate)
    val record = stringResource(R.string.fitness_workout_record_badge)
    val spansDays = workout.secondsByDay().size > 1
    val midnight = stringResource(R.string.fitness_workout_midnight)
    val description =
        listOfNotNull(
            title,
            subtitle,
            date,
            record.takeIf { hasRecord },
            midnight.takeIf { spansDays },
        ).joinToString(". ")
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(ICON_BADGE),
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) { Icon(workout.type.icon, contentDescription = null, tint = accent) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (spansDays) {
                Text(
                    midnight,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DateAndRecord(date, hasRecord, accent)
    }
}

/** Fecha del entreno y, si bate un récord, una copa con la palabra "Récord" (el logro no depende del color). */
@Composable
private fun DateAndRecord(
    date: String,
    hasRecord: Boolean,
    accent: Color,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hasRecord) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
            ) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(BADGE_ICON),
                )
                Text(
                    text = stringResource(R.string.fitness_workout_record_badge),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Resumen de una línea: distancia/kcal/duración en cardio; ejercicios, series y duración en fuerza. */
@Composable
internal fun workoutSummary(
    workout: Workout,
    formatters: FitnessFormatters,
): String {
    val minutes = formatters.minutes(workout.durationSec)
    return if (workout.type.isStrength) {
        val exercises = quantity(R.plurals.fitness_workout_exercises, workout.exercises.size)
        val sets = quantity(R.plurals.fitness_workout_sets, workout.totalSets)
        stringResource(R.string.fitness_workout_meta_strength, exercises, sets, minutes)
    } else {
        val parts =
            listOfNotNull(
                workout.distanceM?.let { stringResource(R.string.fitness_workout_distance, formatters.kilometers(it)) },
                workout.energyKcal?.let { stringResource(R.string.fitness_workout_kcal, formatters.number(it)) },
            )
        val extra = parts.joinToString(" · ")
        if (extra.isEmpty()) {
            stringResource(R.string.fitness_week_total, minutes)
        } else {
            stringResource(R.string.fitness_workout_meta_cardio, extra, minutes)
        }
    }
}
