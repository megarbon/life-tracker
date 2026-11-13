package com.example.lifet.feature.work.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.work.ProjectShare
import com.example.lifet.core.model.work.WorkWeekReport
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.rememberDurationFormat
import com.example.lifet.feature.work.rememberWorkFormatters
import java.text.NumberFormat

private const val ONE_DECIMAL = 1

/** Etiquetas del eje X que caben sin amontonarse. */
private const val MAX_SHARE_BARS = 6

/** Texto "7,5 h" de un valor de gráfica en horas; el número usa el formato del idioma. */
@Composable
private fun rememberHoursText(): (Double) -> String {
    val locale = LocalLocale.current.platformLocale
    val pattern = stringResource(R.string.work_chart_hours_value)
    return remember(locale, pattern) {
        val format = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = ONE_DECIMAL };
        { hours: Double -> pattern.format(format.format(hours)) }
    }
}

/**
 * Horas trabajadas por día de la semana (COLUMNS) con una frase que compara lo acumulado con el objetivo semanal.
 * La tabla accesible ("Ver como tabla") lista las horas de cada día.
 */
@Composable
internal fun HoursChart(
    week: WorkWeekReport,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val durations = rememberDurationFormat()
    val hours = rememberHoursText()
    val points =
        remember(week, formatters, durations) {
            week.days.map { ChartPoint(formatters.shortWeekday(it.date), durations.chartHours(it.workedSeconds)) }
        }
    val headline =
        if (week.workedSeconds == 0L) {
            stringResource(R.string.work_chart_hours_none)
        } else {
            stringResource(
                R.string.work_chart_hours_total,
                durations.format(week.workedSeconds),
                durations.format(week.weeklyTargetSeconds),
            )
        }
    val itemFormat = stringResource(R.string.work_chart_item)
    LTChartCard(
        title = stringResource(R.string.work_chart_hours_title),
        headline = headline,
        summary =
            stringResource(
                R.string.work_chart_hours_summary,
                points.joinToString(", ") { itemFormat.format(it.label, hours(it.value)) },
            ),
        points = points,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.work,
        valueFormatter = hours,
        modifier = modifier,
    )
}

/**
 * Saldo acumulado de horas extra al final de cada semana (LINE). El titular dice si se acumulan horas a favor o a
 * deber, con palabras: el sentido nunca depende sólo del color.
 */
@Composable
internal fun BalanceChart(
    week: WorkWeekReport,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val durations = rememberDurationFormat()
    val hours = rememberHoursText()
    val points =
        remember(week, formatters, durations) {
            week.balances.map {
                ChartPoint(
                    formatters.shortDate(it.weekStart),
                    durations.chartHours(it.cumulativeSeconds),
                )
            }
        }
    val latest = week.balances.lastOrNull()?.cumulativeSeconds ?: 0L
    val hasAnyBalance = week.balances.any { it.balanceSeconds != 0L || it.cumulativeSeconds != 0L }
    val headline =
        when {
            !hasAnyBalance -> stringResource(R.string.work_chart_balance_none)
            latest > 0L -> stringResource(R.string.work_chart_balance_positive, durations.format(latest))
            latest < 0L -> stringResource(R.string.work_chart_balance_negative, durations.format(-latest))
            else -> stringResource(R.string.work_chart_balance_zero)
        }
    val itemFormat = stringResource(R.string.work_chart_item)
    LTChartCard(
        title = stringResource(R.string.work_chart_balance_title),
        headline = headline,
        summary =
            stringResource(
                R.string.work_chart_balance_summary,
                points.joinToString(", ") { itemFormat.format(it.label, hours(it.value)) },
            ),
        points = points,
        kind = ChartKind.LINE,
        color = LifeTrackerTheme.areaColors.work,
        valueFormatter = hours,
        modifier = modifier,
    )
}

/**
 * Reparto de horas por proyecto de las últimas cuatro semanas (barras). El proyecto con más horas va primero y se
 * nombra en el titular; "Sin proyecto" agrupa lo que no tiene proyecto o cuyo proyecto se borró.
 */
@Composable
internal fun ProjectSharesChart(
    shares: List<ProjectShare>,
    modifier: Modifier = Modifier,
    weekly: Boolean = false,
) {
    val durations = rememberDurationFormat()
    val hours = rememberHoursText()
    val none = stringResource(R.string.work_none)
    val points =
        remember(shares, durations, none) {
            shares.take(MAX_SHARE_BARS).map { ChartPoint(it.project?.name ?: none, durations.chartHours(it.seconds)) }
        }
    val top = shares.firstOrNull()
    val headline =
        if (top == null) {
            stringResource(if (weekly) R.string.work_chart_projects_none_week else R.string.work_chart_projects_none)
        } else {
            stringResource(R.string.work_chart_projects_top, top.project?.name ?: none, durations.format(top.seconds))
        }
    val itemFormat = stringResource(R.string.work_chart_item)
    LTChartCard(
        title = stringResource(R.string.work_chart_projects_title),
        headline = headline,
        summary =
            stringResource(
                if (weekly) R.string.work_chart_projects_summary_week else R.string.work_chart_projects_summary,
                points.joinToString(", ") { itemFormat.format(it.label, hours(it.value)) },
            ),
        points = points,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.work,
        valueFormatter = hours,
        modifier = modifier,
    )
}
