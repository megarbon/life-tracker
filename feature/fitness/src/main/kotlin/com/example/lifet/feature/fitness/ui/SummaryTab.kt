package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.component.LTStatTile
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.model.fitness.FitnessTodaySummary
import com.example.lifet.core.model.fitness.PersonalRecord
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.icon
import com.example.lifet.feature.fitness.labelRes
import com.example.lifet.feature.fitness.rememberFitnessFormatters

/** Récords que muestra el resumen. */
private const val RECORDS_SHOWN = 4

/**
 * Resumen: la métrica héroe (minutos activos de la semana frente a los 150 recomendados), la fila de peso, pasos y
 * récords, las dos gráficas principales, el calendario de actividad, las plantillas y los récords recientes.
 */
@Composable
internal fun SummaryTab(
    overview: FitnessOverview,
    contentPadding: PaddingValues,
    onRepeat: (Workout) -> Unit,
    onUseTemplate: (WorkoutTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberFitnessFormatters()
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "hero") { WeeklyHero(overview) }
        item(key = "stats") { StatsRow(overview, formatters) }
        item(key = "chart-weeks") { WeeklyMinutesChart(overview) }
        item(key = "heatmap") { ActivityHeatmapCard(overview) }
        if (overview.recentWorkouts.isNotEmpty() || overview.templates.isNotEmpty()) {
            item(key = "repeat") { RepeatSection(overview, onRepeat, onUseTemplate) }
        }
        item(key = "records-header") { LTSectionHeader(title = stringResource(R.string.fitness_recent_records)) }
        val records =
            overview.history.records
                .takeLast(RECORDS_SHOWN)
                .asReversed()
        if (records.isEmpty()) {
            item(key = "records-empty") {
                Text(
                    text = stringResource(R.string.fitness_recent_records_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(records, key = { "record-${it.workoutId}-${it.exerciseName}-${it.kind}" }) { record ->
                RecordRow(record, formatters)
            }
        }
    }
}

@Composable
private fun WeeklyHero(overview: FitnessOverview) {
    val summary = overview.summary
    LTMetricCard(
        icon = Area.FITNESS.icon,
        title = stringResource(R.string.fitness_hero_title),
        value = stringResource(R.string.fitness_hero_value, summary.weekActiveMinutes),
        subtitle = heroSubtitle(summary, overview.weekWorkouts),
        progress = summary.weekProgress,
        accent = LifeTrackerTheme.areaColors.fitness,
    )
}

@Composable
private fun heroSubtitle(
    summary: FitnessTodaySummary,
    workouts: Int,
): String {
    val goal = stringResource(R.string.fitness_hero_goal, summary.weeklyGoalMinutes)
    val progress =
        if (summary.goalReached) {
            stringResource(R.string.fitness_hero_reached)
        } else {
            stringResource(R.string.fitness_hero_remaining, summary.weeklyGoalMinutes - summary.weekActiveMinutes)
        }
    val today =
        if (summary.workedOutToday) {
            stringResource(R.string.fitness_today_active, summary.todayActiveMinutes)
        } else {
            stringResource(R.string.fitness_today_rest)
        }
    return listOf(
        goal,
        progress,
        pluralStringResource(R.plurals.fitness_hero_workouts, workouts, workouts),
        today,
    ).joinToString("\n")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(
    overview: FitnessOverview,
    formatters: FitnessFormatters,
) {
    val summary = overview.summary
    val none = stringResource(R.string.fitness_stat_none)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        LTStatTile(
            label = stringResource(R.string.fitness_stat_weight),
            value =
                summary.latestWeightGrams?.let { stringResource(R.string.fitness_weight_unit, formatters.kilos(it)) }
                    ?: none,
            supporting = summary.latestWeightDate?.let(formatters::date),
        )
        LTStatTile(
            label = stringResource(R.string.fitness_stat_steps),
            value = summary.stepsToday?.let(formatters::number) ?: none,
            supporting = stringResource(R.string.fitness_stat_steps_goal, formatters.number(overview.stepsGoal)),
        )
        LTStatTile(
            label = stringResource(R.string.fitness_stat_records),
            value = formatters.number(overview.history.records.size),
            supporting = stringResource(R.string.fitness_stat_records_hint),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatSection(
    overview: FitnessOverview,
    onRepeat: (Workout) -> Unit,
    onUseTemplate: (WorkoutTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        LTSectionHeader(title = stringResource(R.string.fitness_recent_workouts))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
            overview.recentWorkouts.forEach { workout ->
                val name = workout.title.ifBlank { stringResource(workout.type.labelRes) }
                SuggestionChip(
                    onClick = { onRepeat(workout) },
                    label = { Text(name, maxLines = 1) },
                    icon = { Icon(workout.type.icon, contentDescription = null) },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
            overview.templates.forEach { template ->
                val name = template.name.ifBlank { stringResource(template.type.labelRes) }
                val description = stringResource(R.string.fitness_template_use, name)
                SuggestionChip(
                    onClick = { onUseTemplate(template) },
                    label = { Text(name, maxLines = 1) },
                    modifier = Modifier.heightIn(min = MinTouchTarget).semantics { contentDescription = description },
                )
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: PersonalRecord,
    formatters: FitnessFormatters,
) {
    val value = recordValue(record, formatters)
    val date = formatters.date(record.date)
    Text(
        text = stringResource(R.string.fitness_record_row, record.exerciseName, value, date),
        style = MaterialTheme.typography.bodyMedium,
    )
}
