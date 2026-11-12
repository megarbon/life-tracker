package com.example.lifet.feature.finance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.shortMonthLabel
import com.example.lifet.feature.finance.spendChangePercent
import com.example.lifet.feature.finance.toChartMoney
import com.example.lifet.feature.finance.toChartValue
import java.time.YearMonth
import kotlin.math.absoluteValue

@Composable
internal fun MonthlySpendChart(
    overview: FinanceOverview,
    money: (Money) -> String,
    accent: Color,
) {
    val labels = overview.monthlyTotals.map { shortMonthLabel(it.month) }
    val points = overview.monthlyTotals.mapIndexed { i, total -> ChartPoint(labels[i], total.spent.toChartValue()) }
    val change = spendChangePercent(overview.monthlyTotals)
    val headline =
        when {
            change == null -> stringResource(R.string.finance_chart_months_none)
            change > 0 -> stringResource(R.string.finance_chart_months_up, change)
            change < 0 -> stringResource(R.string.finance_chart_months_down, change.absoluteValue)
            else -> stringResource(R.string.finance_chart_months_same)
        }
    val itemFormat = stringResource(R.string.finance_chart_summary_item)
    val summary =
        stringResource(
            R.string.finance_chart_months_summary,
            overview.monthlyTotals
                .mapIndexed {
                    i,
                    t,
                    ->
                    itemFormat.format(labels[i], money(t.spent))
                }.joinToString(", "),
        )
    LTChartCard(
        title = stringResource(R.string.finance_chart_months_title),
        headline = headline,
        summary = summary,
        points = points,
        kind = ChartKind.COLUMNS,
        color = accent,
        valueFormatter = chartFormatter(overview.currency, money),
    )
}

@Composable
internal fun BalanceChart(
    overview: FinanceOverview,
    money: (Money) -> String,
    accent: Color,
) {
    val trend = overview.balanceTrend
    val labels = trend.map { shortMonthLabel(YearMonth.from(it.date)) }
    val itemFormat = stringResource(R.string.finance_chart_summary_item)
    val current = trend.lastOrNull()?.balance ?: Money.zero(overview.currency)
    LTChartCard(
        title = stringResource(R.string.finance_chart_balance_title),
        headline = stringResource(R.string.finance_chart_balance_headline, money(current)),
        summary =
            stringResource(
                R.string.finance_chart_balance_summary,
                trend.mapIndexed { i, b -> itemFormat.format(labels[i], money(b.balance)) }.joinToString(", "),
            ),
        points = trend.mapIndexed { i, b -> ChartPoint(labels[i], b.balance.toChartValue()) },
        kind = ChartKind.LINE,
        color = accent,
        valueFormatter = chartFormatter(overview.currency, money),
    )
}

@Composable
private fun chartFormatter(
    currency: CurrencyCode,
    money: (Money) -> String,
): (Double) -> String = remember(currency, money) { { value -> money(value.toChartMoney(currency)) } }
