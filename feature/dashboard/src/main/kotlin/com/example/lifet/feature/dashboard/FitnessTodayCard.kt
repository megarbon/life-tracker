package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.fitness.FitnessTodaySummary
import com.example.lifet.core.ui.icon
import java.text.NumberFormat

private const val PREVIEW_WEEK_MINUTES = 95
private const val PREVIEW_GOAL_MINUTES = 150
private const val PREVIEW_TODAY_MINUTES = 45

/** Tarjeta de Deporte en Hoy: minutos activos de la semana frente al objetivo y si ya has entrenado hoy. */
@Composable
internal fun FitnessTodayCard(
    summary: FitnessTodaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = NumberFormat.getIntegerInstance()
    val subtitle =
        when {
            !summary.hasData -> {
                stringResource(R.string.dashboard_fitness_empty)
            }

            summary.goalReached -> {
                stringResource(R.string.dashboard_fitness_reached)
            }

            summary.workedOutToday -> {
                stringResource(R.string.dashboard_fitness_today, format.format(summary.todayActiveMinutes))
            }

            else -> {
                stringResource(R.string.dashboard_fitness_goal, format.format(summary.weeklyGoalMinutes))
            }
        }
    LTMetricCard(
        icon = Area.FITNESS.icon,
        title = stringResource(R.string.dashboard_fitness_title),
        value = stringResource(R.string.dashboard_fitness_value, format.format(summary.weekActiveMinutes)),
        subtitle = subtitle,
        progress = summary.weekProgress,
        accent = LifeTrackerTheme.areaColors.fitness,
        onClick = onClick,
        modifier = modifier,
    )
}

internal fun previewFitness(): FitnessTodaySummary =
    FitnessTodaySummary(
        weekActiveMinutes = PREVIEW_WEEK_MINUTES,
        weeklyGoalMinutes = PREVIEW_GOAL_MINUTES,
        workedOutToday = true,
        todayActiveMinutes = PREVIEW_TODAY_MINUTES,
        latestWeightGrams = null,
        latestWeightDate = null,
        stepsToday = null,
        hasData = true,
    )

@LTPreviews
@Composable
private fun FitnessTodayCardPreview() {
    LifeTrackerTheme {
        FitnessTodayCard(summary = previewFitness(), onClick = {})
    }
}
