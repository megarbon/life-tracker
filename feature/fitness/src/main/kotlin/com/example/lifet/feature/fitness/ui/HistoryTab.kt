package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.rememberFitnessFormatters
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Una semana del historial con sus entrenos, del más reciente al más antiguo. */
private data class HistoryWeek(
    val monday: LocalDate,
    val minutes: Int,
    val workouts: List<Workout>,
)

/** Historial agrupado por semana (lunes a domingo) con los minutos de cada una. */
@Composable
internal fun HistoryTab(
    overview: FitnessOverview,
    contentPadding: PaddingValues,
    onOpen: (Workout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberFitnessFormatters()
    val weeks = remember(overview.workouts) { groupByWeek(overview.workouts) }
    val recordedWorkouts = remember(overview.history) { overview.history.recordsByWorkout.keys }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        if (weeks.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.fitness_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        weeks.forEach { week ->
            item(key = "week-${week.monday}", contentType = "header") {
                LTSectionHeader(title = weekTitle(week, formatters))
            }
            items(week.workouts, key = { it.id }, contentType = { "workout" }) { workout ->
                WorkoutListItem(
                    workout = workout,
                    formatters = formatters,
                    hasRecord = workout.id in recordedWorkouts,
                    onClick = { onOpen(workout) },
                )
            }
        }
    }
}

@Composable
private fun weekTitle(
    week: HistoryWeek,
    formatters: FitnessFormatters,
): String =
    stringResource(
        R.string.fitness_history_week,
        formatters.date(week.monday),
        stringResource(R.string.fitness_week_total, week.minutes),
    )

private fun groupByWeek(workouts: List<Workout>): List<HistoryWeek> =
    workouts
        .groupBy { it.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .toSortedMap(compareByDescending { it })
        .map { (monday, items) ->
            val minutes = items.sumOf { it.durationSec } / MINUTE_SECONDS
            HistoryWeek(monday, minutes.toInt(), items)
        }

private const val MINUTE_SECONDS = 60L
