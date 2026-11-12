package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.mood.MoodSeries
import com.example.lifet.feature.mood.MoodFormatters
import com.example.lifet.feature.mood.R
import com.example.lifet.feature.mood.emotionLabel
import com.example.lifet.feature.mood.factorLabel
import kotlin.math.abs

/** Por debajo de esta diferencia semanal se considera que el ánimo está estable. */
private const val STABLE_DELTA = 0.2
private const val MAX_FACTORS = 6

/** Las tres gráficas del resumen. Sólo se pintan las que tienen datos suficientes. */
@Composable
internal fun MoodCharts(
    series: MoodSeries,
    formatters: MoodFormatters,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md)) {
        val hasTrend = series.days.any { it.movingAverage != null }
        if (!hasTrend && series.topEmotions.isEmpty()) {
            Text(
                text = stringResource(R.string.mood_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasTrend) TrendChart(series, formatters)
        if (series.topEmotions.isNotEmpty()) EmotionsChart(series)
        if (series.factorImpacts.isNotEmpty()) FactorsChart(series, formatters)
    }
}

@Composable
private fun TrendChart(
    series: MoodSeries,
    formatters: MoodFormatters,
) {
    val points =
        remember(series, formatters) {
            series.days.mapNotNull { day ->
                day.movingAverage?.let { ChartPoint(day.date.format(formatters.shortDate), it) }
            }
        }
    val delta = series.recentDelta
    val headline =
        when {
            delta == null -> stringResource(R.string.mood_chart_trend_none)
            abs(delta) < STABLE_DELTA -> stringResource(R.string.mood_chart_trend_flat)
            delta > 0 -> stringResource(R.string.mood_chart_trend_up, formatters.delta(delta))
            else -> stringResource(R.string.mood_chart_trend_down, formatters.delta(delta))
        }
    val average = series.average?.let(formatters::average) ?: "-"
    LTChartCard(
        title = stringResource(R.string.mood_chart_trend_title),
        headline = headline,
        summary = stringResource(R.string.mood_chart_trend_summary, average),
        points = points,
        kind = ChartKind.LINE,
        color = LifeTrackerTheme.areaColors.mood,
        valueFormatter = formatters::average,
    )
}

@Composable
private fun EmotionsChart(series: MoodSeries) {
    val labels = series.topEmotions.map { emotionLabel(it.emotion) }
    val points = series.topEmotions.mapIndexed { index, count -> ChartPoint(labels[index], count.count.toDouble()) }
    val summary = points.joinToString { "${it.label} ${it.value.toInt()}" }
    LTChartCard(
        title = stringResource(R.string.mood_chart_emotions_title),
        headline = stringResource(R.string.mood_chart_emotions_headline, labels.first()),
        summary = stringResource(R.string.mood_chart_emotions_summary, summary),
        points = points,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.mood,
    )
}

@Composable
private fun FactorsChart(
    series: MoodSeries,
    formatters: MoodFormatters,
) {
    val impacts = series.factorImpacts.take(MAX_FACTORS)
    val labels = impacts.map { factorLabel(it.factor) }
    val points = impacts.mapIndexed { index, impact -> ChartPoint(labels[index], impact.delta) }
    val strongest = impacts.first()
    val summary = points.joinToString { "${it.label} ${formatters.delta(it.value)}" }
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        LTChartCard(
            title = stringResource(R.string.mood_chart_factors_title),
            headline =
                stringResource(
                    R.string.mood_chart_factors_headline,
                    labels.first().lowercase(),
                    formatters.delta(strongest.delta),
                ),
            summary = stringResource(R.string.mood_chart_factors_summary, summary),
            points = points,
            kind = ChartKind.COLUMNS,
            color = LifeTrackerTheme.areaColors.mood,
            valueFormatter = formatters::delta,
        )
        Text(
            text = stringResource(R.string.mood_chart_factors_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
