package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.ui.icon
import java.text.NumberFormat

private const val MAX_LEVEL = 5f

/** Tarjeta de Ánimo en Hoy: media de hoy o invitación al check-in; toca para abrir el área. */
@Composable
internal fun MoodTodayCard(
    summary: MoodTodaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    val average = summary.averageToday
    val value =
        if (average != null) {
            stringResource(R.string.dashboard_mood_value, format.format(average))
        } else {
            stringResource(R.string.dashboard_mood_missing_value)
        }
    val subtitle =
        when {
            summary.missingCheckIn -> {
                stringResource(R.string.dashboard_mood_missing)
            }

            summary.weekAverage != null -> {
                stringResource(R.string.dashboard_mood_week, format.format(summary.weekAverage))
            }

            else -> {
                null
            }
        }
    LTMetricCard(
        icon = Area.MOOD.icon,
        title = stringResource(R.string.dashboard_mood_title),
        value = value,
        subtitle = subtitle,
        progress = average?.let { (it / MAX_LEVEL).toFloat() },
        accent = LifeTrackerTheme.areaColors.mood,
        onClick = onClick,
        modifier = modifier,
    )
}

@LTPreviews
@Composable
private fun MoodTodayCardPreview() {
    LifeTrackerTheme {
        MoodTodayCard(
            summary =
                MoodTodaySummary(
                    averageToday = 3.5,
                    checkInsToday = 2,
                    weekAverage = 3.2,
                    lastLevel = MoodLevel.GOOD,
                    lowStreakDays = 0,
                ),
            onClick = {},
        )
    }
}
