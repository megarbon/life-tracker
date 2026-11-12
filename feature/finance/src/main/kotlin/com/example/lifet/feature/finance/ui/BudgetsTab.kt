package com.example.lifet.feature.finance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.CategorySpend
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.displayName
import com.example.lifet.feature.finance.iconFor
import com.example.lifet.feature.finance.rememberMoneyFormatter

/** Un presupuesto mensual por categoría de gasto; tocar una fila abre la hoja para fijar el límite. */
@Composable
internal fun BudgetsTab(
    overview: FinanceOverview,
    contentPadding: PaddingValues,
    onEditBudget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter()
    val spendById = overview.summary.byCategory.associateBy { it.category.id }
    val budgets = overview.budgets.associateBy { it.categoryId }
    val expenseCategories = overview.categories.filter { it.kind == CategoryKind.EXPENSE && !it.archived }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        item(key = "intro") { MutedText(stringResource(R.string.finance_budgets_intro)) }
        items(expenseCategories, key = { "budget-${it.id}" }) { category ->
            BudgetRow(
                category = category,
                spend = spendById[category.id],
                limit = budgets[category.id]?.limit,
                money = money,
                onClick = { onEditBudget(category.id) },
            )
        }
    }
}

@Composable
private fun BudgetRow(
    category: FinanceCategory,
    spend: CategorySpend?,
    limit: Money?,
    money: (Money) -> String,
    onClick: () -> Unit,
) {
    val name = category.displayName()
    val detail =
        if (limit == null) {
            stringResource(R.string.finance_budget_row_none)
        } else {
            stringResource(
                R.string.finance_category_row_budget,
                money(spend?.spent ?: Money.zero(limit.currency)),
                money(limit),
            )
        }
    val color = if (spend?.isOver == true) MaterialTheme.colorScheme.error else LifeTrackerTheme.areaColors.finance
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = "$name. $detail"
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(category.iconKey), contentDescription = null, tint = color)
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
        if (limit != null) {
            LinearProgressIndicator(
                progress = { (spend?.ratio ?: 0f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
