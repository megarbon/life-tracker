package com.example.lifet.feature.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.nutrition.MacroSplit
import com.example.lifet.core.model.nutrition.NutritionSeries
import com.example.lifet.feature.nutrition.R

/** kcal de los últimos 7 días (columnas) con el objetivo en el titular. */
@Composable
internal fun WeekKcalChart(
    series: NutritionSeries,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val points = remember(series, locale) { series.dailyKcalPoints(locale) }
    val average = series.averageKcal
    val goal = format.kcal(series.goals.kcal.toDouble())
    val logged = stringResource(R.string.nutrition_logged_days, series.loggedDayCount, series.days.size)
    LTChartCard(
        title = stringResource(R.string.nutrition_week_chart_title),
        headline =
            if (average == null) {
                stringResource(R.string.nutrition_week_chart_empty)
            } else {
                stringResource(R.string.nutrition_week_chart_headline, format.kcal(average), goal)
            },
        summary = stringResource(R.string.nutrition_week_chart_summary, goal, logged),
        points = points,
        modifier = modifier,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.nutrition,
        valueFormatter = { format.kcal(it) },
    )
}

/** Tendencia de 30 días: media móvil de 7 días (línea) frente al objetivo. */
@Composable
internal fun TrendChart(
    series: NutritionSeries,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val points = remember(series, locale) { series.trendPoints(locale) }
    val goal = format.kcal(series.goals.kcal.toDouble())
    val logged = stringResource(R.string.nutrition_logged_days, series.loggedDayCount, series.days.size)
    LTChartCard(
        title = stringResource(R.string.nutrition_trend_title),
        headline =
            points.lastOrNull()?.let { stringResource(R.string.nutrition_trend_headline, format.kcal(it.value), goal) }
                ?: stringResource(R.string.nutrition_trend_empty),
        summary = stringResource(R.string.nutrition_trend_summary, goal, logged),
        points = points,
        modifier = modifier,
        kind = ChartKind.LINE,
        color = LifeTrackerTheme.areaColors.nutrition,
        valueFormatter = { format.kcal(it) },
    )
}

/** Reparto de la energía entre proteínas, hidratos y grasas como tres barras horizontales. */
@Composable
internal fun MacroSplitCard(
    split: MacroSplit,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val colors = LifeTrackerTheme.areaColors
    val protein = format.percent(split.protein)
    val carbs = format.percent(split.carbs)
    val fat = format.percent(split.fat)
    val hasData = split != MacroSplit.ZERO
    val headline =
        if (hasData) {
            stringResource(R.string.nutrition_macros_headline, protein, carbs, fat)
        } else {
            stringResource(R.string.nutrition_macros_empty)
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.nutrition_macros_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = headline, style = MaterialTheme.typography.titleMedium)
            if (hasData) {
                val summary = stringResource(R.string.nutrition_macros_summary, headline)
                Column(
                    verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = summary },
                ) {
                    ShareBar(stringResource(R.string.nutrition_macro_protein), split.protein, protein, colors.fitness)
                    ShareBar(stringResource(R.string.nutrition_macro_carbs), split.carbs, carbs, colors.nutrition)
                    ShareBar(stringResource(R.string.nutrition_macro_fat), split.fat, fat, colors.work)
                }
            }
        }
    }
}

@Composable
private fun ShareBar(
    label: String,
    fraction: Double,
    percent: String,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = stringResource(R.string.nutrition_percent_value, percent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
