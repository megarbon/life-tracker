package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.util.format
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.FinanceTodaySummary
import com.example.lifet.core.ui.icon

private const val PREVIEW_SPENT = 1_250L
private const val PREVIEW_AVAILABLE = 124_050L
private const val PREVIEW_BUDGET_LEFT = 34_000L

/** Tarjeta de Finanzas en Hoy: disponible este mes, gasto de hoy y presupuesto restante. */
@Composable
internal fun FinanceTodayCard(
    summary: FinanceTodaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val budgetRemaining = summary.budgetRemaining
    val subtitle =
        when {
            !summary.hasData -> {
                stringResource(R.string.dashboard_finance_empty)
            }

            budgetRemaining != null -> {
                stringResource(
                    R.string.dashboard_finance_today_budget,
                    summary.spentToday.format(),
                    budgetRemaining.format(),
                )
            }

            else -> {
                stringResource(R.string.dashboard_finance_today, summary.spentToday.format())
            }
        }
    LTMetricCard(
        icon = Area.FINANCE.icon,
        title = stringResource(R.string.dashboard_finance_title),
        value = summary.availableThisMonth.format(),
        subtitle = subtitle,
        accent = LifeTrackerTheme.areaColors.finance,
        onClick = onClick,
        modifier = modifier,
    )
}

@LTPreviews
@Composable
private fun FinanceTodayCardPreview() {
    LifeTrackerTheme {
        val eur = CurrencyCode.EUR
        FinanceTodayCard(
            summary =
                FinanceTodaySummary(
                    currency = eur,
                    spentToday = Money(PREVIEW_SPENT, eur),
                    availableThisMonth = Money(PREVIEW_AVAILABLE, eur),
                    budgetRemaining = Money(PREVIEW_BUDGET_LEFT, eur),
                    hasData = true,
                ),
            onClick = {},
        )
    }
}
