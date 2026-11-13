package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTCalendarHeatmap
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.quit.HabitProgress
import com.example.lifet.core.model.quit.QuitRules
import com.example.lifet.feature.quit.QuitFormatters
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.rememberQuitFormatters
import com.example.lifet.feature.quit.toChartMoney
import com.example.lifet.feature.quit.toChartValue
import java.time.LocalDate
import java.time.YearMonth

/** Horas que agrupa cada columna de la gráfica de antojos: 24 columnas no caben con la fuente grande. */
private const val BLOCK_HOURS = 3
private const val HOURS_IN_DAY = 24

/**
 * Antojos a lo largo del día (COLUMNS), en franjas de tres horas. El titular dice a qué hora llegan más; si aún no hay
 * antojos, invita a anotarlos en lugar de enseñar una gráfica vacía sin explicación.
 */
@Composable
internal fun CravingHoursChart(
    progress: HabitProgress,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberQuitFormatters()
    val blocks = remember(progress.cravingsByHour) { progress.cravingsByBlock(BLOCK_HOURS) }
    val labelFormat = stringResource(R.string.quit_chart_hour_label)
    val points =
        remember(blocks, labelFormat) {
            blocks.mapIndexed { index, count -> ChartPoint(labelFormat.format(index * BLOCK_HOURS), count.toDouble()) }
        }
    val peak = blocks.indices.maxByOrNull { blocks[it] }?.takeIf { blocks[it] > 0 }
    val headline =
        if (peak == null) {
            stringResource(R.string.quit_chart_hours_none)
        } else {
            stringResource(
                R.string.quit_chart_hours_peak,
                formatters.hour(peak * BLOCK_HOURS),
                formatters.hour((peak + 1) * BLOCK_HOURS % HOURS_IN_DAY),
            )
        }
    val itemFormat = stringResource(R.string.quit_chart_item)
    LTChartCard(
        title = stringResource(R.string.quit_chart_hours_title),
        headline = headline,
        summary =
            stringResource(
                R.string.quit_chart_hours_summary,
                points.joinToString(", ") { itemFormat.format(it.label, it.value.toInt()) },
            ),
        points = points,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.quit,
        modifier = modifier,
    )
}

/** Ahorro acumulado desde que empezó el hábito (LINE): nunca baja, porque un tropiezo no resta lo ya ahorrado. */
@Composable
internal fun SavingsChart(
    progress: HabitProgress,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberQuitFormatters()
    val currency = progress.habit.packPrice.currency
    val points =
        remember(progress.savings, formatters) {
            progress.savings.map { ChartPoint(it.date.format(formatters.shortDate), it.saved.toChartValue()) }
        }
    val money: (Double) -> String = { value -> formatters.money(value.toChartMoney(currency)) }
    val headline =
        if (progress.saved.isZero) {
            stringResource(R.string.quit_chart_savings_none)
        } else {
            stringResource(R.string.quit_chart_savings_up, formatters.money(progress.saved))
        }
    val itemFormat = stringResource(R.string.quit_chart_item)
    LTChartCard(
        title = stringResource(R.string.quit_chart_savings_title),
        headline = headline,
        summary =
            stringResource(
                R.string.quit_chart_savings_summary,
                points.joinToString(", ") { itemFormat.format(it.label, money(it.value)) },
            ),
        points = points,
        kind = ChartKind.LINE,
        color = LifeTrackerTheme.areaColors.quit,
        valueFormatter = money,
        modifier = modifier,
    )
}

/** Consumo de las últimas semanas frente al límite (COLUMNS), sólo en modo reducción. */
@Composable
internal fun WeeklyUseChart(
    progress: HabitProgress,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberQuitFormatters()
    val points =
        remember(progress.weeks, formatters) {
            progress.weeks.map { ChartPoint(it.weekStart.format(formatters.shortDate), it.units.toDouble()) }
        }
    val week = progress.thisWeek
    val limit = week?.limit
    val headline =
        when {
            week == null -> stringResource(R.string.quit_reduce_compliance_none)
            limit == null -> stringResource(R.string.quit_reduce_week_value_no_limit, week.units)
            week.compliant -> stringResource(R.string.quit_chart_week_within, limit)
            else -> stringResource(R.string.quit_chart_week_at, week.units, limit)
        }
    val itemFormat = stringResource(R.string.quit_chart_item)
    LTChartCard(
        title = stringResource(R.string.quit_chart_week_title),
        headline = headline,
        summary =
            stringResource(
                R.string.quit_chart_week_summary,
                points.joinToString(", ") { itemFormat.format(it.label, it.value.toInt()) },
            ),
        points = points,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.quit,
        modifier = modifier,
    )
}

/**
 * Calendario de días limpios de los últimos meses. [LTCalendarHeatmap] dibuja un mes, así que se compone mes a mes con
 * su título. Un día con algún consumo se pinta más tenue (no vacío ni de otro color): no resta, sólo se distingue.
 */
@Composable
internal fun CleanDaysHeatmapCard(
    progress: HabitProgress,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberQuitFormatters()
    val months = remember(today) { heatmapMonths(today) }
    val cleanDays = progress.intensity.values.count { it >= 1f }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.quit_heatmap_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.quit_heatmap_headline,
                        cleanDays,
                        cleanDays,
                        QuitRules.HEATMAP_MONTHS,
                    ),
                style = MaterialTheme.typography.titleMedium,
            )
            months.forEach { month -> HeatmapMonth(month, progress, formatters) }
            Text(
                text = stringResource(R.string.quit_heatmap_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeatmapMonth(
    month: YearMonth,
    progress: HabitProgress,
    formatters: QuitFormatters,
) {
    val title = formatters.monthTitle(month)
    val clean = progress.intensity.filter { (date, level) -> YearMonth.from(date) == month && level >= 1f }.size
    Text(text = title, style = MaterialTheme.typography.labelLarge)
    LTCalendarHeatmap(
        range = DateRange.containing(DateRangeUnit.MONTH, month.atDay(1)),
        intensity = progress.intensity,
        summary = pluralStringResource(R.plurals.quit_heatmap_month_summary, clean, title, clean),
        color = LifeTrackerTheme.areaColors.quit,
    )
}

/** El mes actual y los anteriores hasta cubrir [QuitRules.HEATMAP_MONTHS], del más antiguo al actual. */
internal fun heatmapMonths(today: LocalDate): List<YearMonth> =
    (QuitRules.HEATMAP_MONTHS - 1 downTo 0).map { YearMonth.from(today).minusMonths(it.toLong()) }
