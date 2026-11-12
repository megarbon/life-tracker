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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.displayName
import com.example.lifet.feature.finance.iconFor
import com.example.lifet.feature.finance.labelRes
import com.example.lifet.feature.finance.rememberMoneyFormatter

/** Cuentas, categorías y recurrentes: lo que se configura una vez y rara vez se toca. */
@Composable
internal fun AccountsTab(
    overview: FinanceOverview,
    contentPadding: PaddingValues,
    onEditAccount: (Account?) -> Unit,
    onEditCategory: (FinanceCategory?) -> Unit,
    onEditRule: (RecurringRule?) -> Unit,
    onAddTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter()
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "accounts-header") { LTSectionHeader(title = stringResource(R.string.finance_accounts_header)) }
        items(overview.accounts, key = { "account-${it.account.id}" }) { item ->
            ConfigRow(
                icon = item.account.type.icon(),
                title = item.account.displayName(),
                detail = money(item.balance),
                caption =
                    stringResource(item.account.type.labelRes()) +
                        if (item.account.archived) " · " + stringResource(R.string.finance_archived) else "",
                onClick = { onEditAccount(item.account) },
            )
        }
        item(key = "accounts-add") {
            Row {
                TextButton(onClick = { onEditAccount(null) }) { Text(stringResource(R.string.finance_account_add)) }
                if (overview.accounts.count { !it.account.archived } > 1) {
                    TextButton(onClick = onAddTransfer) { Text(stringResource(R.string.finance_add_transfer)) }
                }
            }
        }
        item(key = "rules-header") { LTSectionHeader(title = stringResource(R.string.finance_rules_header)) }
        items(overview.rules, key = { "rule-${it.id}" }) { rule ->
            val category = overview.categories.firstOrNull { it.id == rule.categoryId }
            ConfigRow(
                icon = iconFor(category?.iconKey.orEmpty()),
                title = rule.name,
                detail = money(rule.amount),
                caption = stringResource(rule.frequency.labelRes()),
                onClick = { onEditRule(rule) },
            )
        }
        item(key = "rules-add") {
            TextButton(onClick = { onEditRule(null) }) { Text(stringResource(R.string.finance_rule_add)) }
        }
        item(key = "categories-header") { LTSectionHeader(title = stringResource(R.string.finance_categories_header)) }
        items(overview.categories, key = { "category-${it.id}" }) { category ->
            ConfigRow(
                icon = iconFor(category.iconKey),
                title = category.displayName(),
                detail = "",
                caption =
                    stringResource(
                        if (category.kind == CategoryKind.EXPENSE) {
                            R.string.finance_entry_expense
                        } else {
                            R.string.finance_entry_income
                        },
                    ) + if (category.archived) " · " + stringResource(R.string.finance_archived) else "",
                onClick = { onEditCategory(category) },
            )
        }
        item(key = "categories-add") {
            TextButton(onClick = { onEditCategory(null) }) { Text(stringResource(R.string.finance_category_add)) }
        }
    }
}

@Composable
private fun ConfigRow(
    icon: ImageVector,
    title: String,
    detail: String,
    caption: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = listOf(title, caption, detail).filter { it.isNotBlank() }.joinToString(". ")
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = LifeTrackerTheme.areaColors.finance)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.titleMedium)
    }
}

private fun AccountType.icon(): ImageVector =
    when (this) {
        AccountType.CASH -> Icons.Rounded.Payments
        AccountType.BANK -> Icons.Rounded.AccountBalance
        AccountType.CARD -> Icons.Rounded.CreditCard
        AccountType.SAVINGS -> Icons.Rounded.Savings
    }
