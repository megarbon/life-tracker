package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.component.LTDateRangeSelector
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.mood.MoodAction
import com.example.lifet.feature.mood.MoodFormatters
import com.example.lifet.feature.mood.MoodUiState
import com.example.lifet.feature.mood.R

private const val SCALE_MAX = 5f

/** Resumen: héroe, check-in rápido, apoyo (si procede) y gráficas del rango elegido. */
@Composable
internal fun MoodOverviewTab(
    state: MoodUiState.Success,
    formatters: MoodFormatters,
    onAction: (MoodAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "hero") { MoodHero(state.summary, formatters) }
        item(key = "quick") { QuickCheckInCard(onAction) }
        if (state.supportVisible) {
            item(key = "support") { MoodSupportCard(onDismiss = { onAction(MoodAction.DismissSupport) }) }
        }
        item(key = "range") {
            LTDateRangeSelector(
                range = state.range,
                today = state.today,
                onRangeChange = { onAction(MoodAction.ChangeRange(it)) },
            )
        }
        item(key = "charts") { MoodCharts(state.series, formatters) }
    }
}

@Composable
private fun MoodHero(
    summary: MoodTodaySummary,
    formatters: MoodFormatters,
) {
    val average = summary.averageToday
    val value =
        average?.let { stringResource(R.string.mood_hero_value, formatters.average(it)) }
            ?: stringResource(R.string.mood_hero_missing_value)
    val context =
        if (summary.missingCheckIn) {
            stringResource(R.string.mood_hero_missing)
        } else {
            pluralStringResource(R.plurals.mood_check_ins_today, summary.checkInsToday, summary.checkInsToday)
        }
    val week = summary.weekAverage?.let { stringResource(R.string.mood_hero_week, formatters.average(it)) }
    LTMetricCard(
        icon = Area.MOOD.icon,
        title = stringResource(R.string.mood_hero_title),
        value = value,
        subtitle = listOfNotNull(context, week).joinToString("\n"),
        progress = average?.let { (it / SCALE_MAX).toFloat() },
        accent = LifeTrackerTheme.areaColors.mood,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QuickCheckInCard(onAction: (MoodAction) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.mood_quick_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            MoodLevelPicker(
                selected = null,
                onSelect = { level ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onAction(MoodAction.QuickCheckIn(level))
                },
            )
            Text(
                text = stringResource(R.string.mood_quick_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
