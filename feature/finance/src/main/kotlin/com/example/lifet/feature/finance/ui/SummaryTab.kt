package com.example.lifet.feature.finance.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.designsystem.util.rememberReducedMotion
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.CategorySpend
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.displayName
import com.example.lifet.feature.finance.iconFor
import com.example.lifet.feature.finance.mediumDate
import com.example.lifet.feature.finance.rememberMoneyFormatter
import com.example.lifet.feature.finance.shortMonthLabel
import com.example.lifet.feature.finance.spendChangePercent
import com.example.lifet.feature.finance.toChartMoney
import com.example.lifet.feature.finance.toChartValue
import java.time.YearMonth
import kotlin.math.absoluteValue

private const val PROGRESS_ANIMATION_MS = 600

/** Resumen del mes: disponible (héroe), presupuesto, gráficas, reparto por categoría y próximos recurrentes. */
@Composable
internal fun SummaryTab(
    overview: FinanceOverview,
    contentPadding: PaddingValues,
    onOpenBudgets: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter()
    val accent = LifeTrackerTheme.areaColors.finance
    val spacing = LifeTrackerTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "hero") { AvailableHero(overview, money) }
        if (overview.otherCurrencies.isNotEmpty()) {
            item(key = "currencies") { OtherCurrenciesNote(overview.otherCurrencies, overview.currency) }
        }
        item(key = "budget") { BudgetCard(overview, money, onOpenBudgets) }
        item(key = "chart-months") { MonthlySpendChart(overview, money, accent) }
        item(key = "chart-balance") { BalanceChart(overview, money, accent) }
        item(key = "categories-header") { LTSectionHeader(title = stringResource(R.string.finance_by_category)) }
        val spending = overview.summary.byCategory.filter { it.spent.amountMinor > 0L || it.budget != null }
        if (spending.isEmpty()) {
            item(key = "categories-empty") { MutedText(stringResource(R.string.finance_by_category_empty)) }
        } else {
            val total = spending.sumOf { it.spent.amountMinor.coerceAtLeast(0L) }
            items(spending, key = { "cat-${it.category.id}" }) { CategoryRow(it, total, money) }
        }
        item(key = "upcoming-header") { LTSectionHeader(title = stringResource(R.string.finance_upcoming)) }
        if (overview.upcoming.isEmpty()) {
            item(key = "upcoming-empty") { MutedText(stringResource(R.string.finance_upcoming_empty)) }
        } else {
            items(overview.upcoming, key = { "up-${it.id}" }) { tx ->
                UpcomingRow(tx, overview, money, onClick = { onEditTransaction(tx) })
            }
        }
    }
}

@Composable
private fun AvailableHero(
    overview: FinanceOverview,
    money: (Money) -> String,
) {
    val summary = overview.summary
    val planned =
        if (summary.plannedSpent.isZero && summary.plannedIncome.isZero) {
            null
        } else {
            stringResource(
                R.string.finance_available_planned,
                money(summary.plannedSpent),
                money(summary.plannedIncome),
            )
        }
    LTMetricCard(
        icon = Area.FINANCE.icon,
        title = stringResource(R.string.finance_available_title),
        value = money(summary.available),
        subtitle =
            listOfNotNull(
                stringResource(R.string.finance_available_subtitle, money(summary.income), money(summary.spent)),
                planned,
            ).joinToString("\n"),
        accent = LifeTrackerTheme.areaColors.finance,
    )
}

@Composable
private fun BudgetCard(
    overview: FinanceOverview,
    money: (Money) -> String,
    onOpenBudgets: () -> Unit,
) {
    val summary = overview.summary
    val total = summary.budgetTotal
    val remaining = summary.budgetRemaining
    if (total == null || remaining == null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md)) {
                Text(stringResource(R.string.finance_budget_card_title), style = MaterialTheme.typography.titleSmall)
                MutedText(stringResource(R.string.finance_budget_none))
                TextButton(onClick = onOpenBudgets) { Text(stringResource(R.string.finance_budget_set_cta)) }
            }
        }
        return
    }
    val over = remaining.isNegative
    val subtitle =
        if (over) {
            stringResource(R.string.finance_budget_over, money(-remaining), money(total))
        } else {
            stringResource(R.string.finance_budget_left, money(remaining), money(total))
        }
    LTMetricCard(
        icon = iconFor("savings"),
        title = stringResource(R.string.finance_budget_card_title),
        value = money(remaining),
        subtitle = subtitle,
        progress = ratio(summary.budgetSpent, total),
        accent = if (over) MaterialTheme.colorScheme.error else LifeTrackerTheme.areaColors.finance,
        onClick = onOpenBudgets,
    )
}

/** Fila del reparto por categoría con barra de progreso (contra el presupuesto o contra el gasto total). */
@Composable
private fun CategoryRow(
    spend: CategorySpend,
    totalSpent: Long,
    money: (Money) -> String,
) {
    val name = spend.category.displayName()
    val budget = spend.budget
    val progress =
        if (budget != null) {
            spend.ratio
        } else if (totalSpent > 0L) {
            spend.spent.amountMinor
                .coerceAtLeast(0L)
                .toFloat() / totalSpent
        } else {
            0f
        }
    val detail =
        when {
            spend.isOver && budget != null -> {
                stringResource(R.string.finance_category_row_budget, money(spend.spent), money(budget)) + " · " +
                    stringResource(R.string.finance_category_row_over)
            }

            budget != null -> {
                stringResource(R.string.finance_category_row_budget, money(spend.spent), money(budget))
            }

            else -> {
                money(spend.spent)
            }
        }
    val color = if (spend.isOver) MaterialTheme.colorScheme.error else LifeTrackerTheme.areaColors.finance
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clearAndSetSemantics { contentDescription = "$name. $detail" },
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(spend.category.iconKey), contentDescription = null, tint = color)
            Text(
                text = name,
                modifier = Modifier.weight(1f).padding(horizontal = LifeTrackerTheme.spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedBar(progress, color)
    }
}

@Composable
private fun AnimatedBar(
    target: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    val reduced = rememberReducedMotion()
    var started by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) target.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(if (reduced) 0 else PROGRESS_ANIMATION_MS),
        label = "category-progress",
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

@Composable
private fun UpcomingRow(
    transaction: FinanceTransaction,
    overview: FinanceOverview,
    money: (Money) -> String,
    onClick: () -> Unit,
) {
    val category = overview.categories.firstOrNull { it.id == transaction.categoryId }
    TransactionListItem(
        title = transaction.note.ifBlank { category?.displayName().orEmpty() },
        subtitle = mediumDate(transaction.date) + " · " + stringResource(R.string.finance_planned),
        amount = money(transaction.amount),
        icon = iconFor(category?.iconKey.orEmpty()),
        positive = !transaction.amount.isNegative,
        planned = true,
        onClick = onClick,
    )
}

@Composable
private fun OtherCurrenciesNote(
    currencies: Set<CurrencyCode>,
    primary: CurrencyCode,
) {
    MutedText(
        stringResource(
            R.string.finance_other_currencies,
            currencies.sortedBy { it.value }.joinToString { it.value },
            primary.value,
        ),
        modifier = Modifier.semantics { },
    )
}

@Composable
internal fun MutedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = LifeTrackerTheme.spacing.xs),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun ratio(
    spent: Money,
    total: Money,
): Float =
    when {
        total.amountMinor > 0L -> (spent.amountMinor.coerceAtLeast(0L).toDouble() / total.amountMinor).toFloat()
        spent.amountMinor > 0L -> 1f
        else -> 0f
    }
