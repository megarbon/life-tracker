package com.example.lifet.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.ui.icon
import java.text.NumberFormat

private const val MAX_LEVEL = 5f

private val MoodLevel.faceIcon: ImageVector
    get() =
        when (this) {
            MoodLevel.VERY_LOW -> Icons.Rounded.SentimentVeryDissatisfied
            MoodLevel.LOW -> Icons.Rounded.SentimentDissatisfied
            MoodLevel.NEUTRAL -> Icons.Rounded.SentimentNeutral
            MoodLevel.GOOD -> Icons.Rounded.SentimentSatisfied
            MoodLevel.VERY_GOOD -> Icons.Rounded.SentimentVerySatisfied
        }

@get:StringRes
private val MoodLevel.faceLabel: Int
    get() =
        when (this) {
            MoodLevel.VERY_LOW -> R.string.dashboard_mood_level_very_low
            MoodLevel.LOW -> R.string.dashboard_mood_level_low
            MoodLevel.NEUTRAL -> R.string.dashboard_mood_level_neutral
            MoodLevel.GOOD -> R.string.dashboard_mood_level_good
            MoodLevel.VERY_GOOD -> R.string.dashboard_mood_level_very_good
        }

/**
 * Tarjeta de Ánimo en Hoy: media de hoy y cinco caritas para hacer el check-in con un toque sin salir de Hoy.
 * El botón inferior abre el área completa.
 */
@Composable
internal fun MoodTodayCard(
    summary: MoodTodaySummary,
    onOpen: () -> Unit,
    onCheckIn: (MoodLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LifeTrackerTheme.areaColors.mood
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            MoodHeader(value = value, subtitle = subtitle, average = average, accent = accent)
            QuickMoodRow(onCheckIn = onCheckIn, accent = accent)
            TextButton(onClick = onOpen) { Text(stringResource(R.string.dashboard_mood_open)) }
        }
    }
}

@Composable
private fun MoodHeader(
    value: String,
    subtitle: String?,
    average: Double?,
    accent: Color,
) {
    Column(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(value, subtitle).joinToString(". ")
            },
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Icon(Area.MOOD.icon, contentDescription = null, tint = accent)
            Text(
                text = stringResource(R.string.dashboard_mood_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(value, style = MaterialTheme.typography.displaySmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (average != null) {
            LinearProgressIndicator(
                progress = { (average / MAX_LEVEL).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
private fun QuickMoodRow(
    onCheckIn: (MoodLevel) -> Unit,
    accent: Color,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MoodLevel.entries.forEach { level ->
            val label = stringResource(level.faceLabel)
            IconButton(
                onClick = { onCheckIn(level) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(
                    imageVector = level.faceIcon,
                    contentDescription = stringResource(R.string.dashboard_mood_checkin, label),
                    tint = accent,
                )
            }
        }
    }
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
            onOpen = {},
            onCheckIn = {},
        )
    }
}
