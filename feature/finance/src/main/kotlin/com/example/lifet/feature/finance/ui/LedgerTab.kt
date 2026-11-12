package com.example.lifet.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTMultiChipGroup
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.DayGroup
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.LedgerFilter
import com.example.lifet.core.model.finance.TransactionEntry
import com.example.lifet.core.model.finance.TransferEntry
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.displayName
import com.example.lifet.feature.finance.fullDate
import com.example.lifet.feature.finance.iconFor
import com.example.lifet.feature.finance.rememberMoneyFormatter

/** Movimientos del mes agrupados por día con su total, más búsqueda (en todo el historial) y filtros. */
@Composable
internal fun LedgerTab(
    overview: FinanceOverview,
    filter: LedgerFilter,
    contentPadding: PaddingValues,
    onFilterChange: (LedgerFilter) -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onEditTransfer: (FinanceTransfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter()
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "filters") { LedgerFilters(overview, filter, onFilterChange) }
        if (overview.ledger.isEmpty()) {
            item(key = "empty") { LedgerEmpty(filter, onClear = { onFilterChange(LedgerFilter()) }) }
        }
        overview.ledger.forEach { group ->
            item(key = "day-${group.date}") { DayHeader(group, money) }
            items(group.entries, key = { "entry-${it.id}" }) { entry ->
                when (entry) {
                    is TransactionEntry -> TransactionRow(entry.transaction, overview, money, onEditTransaction)
                    is TransferEntry -> TransferRow(entry.transfer, overview, money, onEditTransfer)
                }
            }
        }
    }
}

@Composable
private fun LedgerFilters(
    overview: FinanceOverview,
    filter: LedgerFilter,
    onFilterChange: (LedgerFilter) -> Unit,
) {
    val spacing = LifeTrackerTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = { onFilterChange(filter.copy(query = it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.finance_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (filter.query.isNotEmpty()) {
                    IconButton(onClick = { onFilterChange(filter.copy(query = "")) }) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.finance_search_clear))
                    }
                }
            },
            singleLine = true,
        )
        if (filter.searchesHistory) MutedText(stringResource(R.string.finance_search_scope))
        val kinds = listOf<CategoryKind?>(null, CategoryKind.EXPENSE, CategoryKind.INCOME)
        val kindLabels =
            mapOf(
                null to stringResource(R.string.finance_tab_ledger),
                CategoryKind.EXPENSE to stringResource(R.string.finance_entry_expense),
                CategoryKind.INCOME to stringResource(R.string.finance_entry_income),
            )
        LTChipGroup(
            options = kinds,
            selected = filter.kind,
            label = { kindLabels[it].orEmpty() },
            onSelect = { onFilterChange(filter.copy(kind = it, categoryIds = emptySet())) },
        )
        val categories =
            overview.categories.filter { !it.archived && (filter.kind == null || it.kind == filter.kind) }
        val categoryNames = categories.associate { it.id to it.displayName() }
        LTMultiChipGroup(
            options = categories.map { it.id },
            selected = filter.categoryIds,
            label = { categoryNames[it].orEmpty() },
            onToggle = { id -> onFilterChange(filter.copy(categoryIds = filter.categoryIds.toggle(id))) },
        )
        if (overview.accounts.size > 1) {
            val accountNames = overview.accounts.associate { it.account.id to it.account.displayName() }
            LTMultiChipGroup(
                options = overview.accounts.map { it.account.id },
                selected = filter.accountIds,
                label = { accountNames[it].orEmpty() },
                onToggle = { id -> onFilterChange(filter.copy(accountIds = filter.accountIds.toggle(id))) },
            )
        }
        if (filter.isActive) {
            TextButton(onClick = { onFilterChange(LedgerFilter()) }) {
                Text(stringResource(R.string.finance_filters_clear))
            }
        }
    }
}

@Composable
private fun LedgerEmpty(
    filter: LedgerFilter,
    onClear: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = EMPTY_MIN_HEIGHT)) {
        if (filter.isActive) {
            LTEmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(R.string.finance_no_results_title),
                message = stringResource(R.string.finance_no_results_message),
                actionLabel = stringResource(R.string.finance_filters_clear),
                onAction = onClear,
            )
        } else {
            LTEmptyState(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                title = stringResource(R.string.finance_ledger_empty_title),
                message = stringResource(R.string.finance_ledger_empty_message),
            )
        }
    }
}

@Composable
private fun DayHeader(
    group: DayGroup,
    money: (Money) -> String,
) {
    val date = fullDate(group.date)
    val total = group.total?.let(money).orEmpty()
    val description = stringResource(R.string.finance_day_total_cd, date, total)
    Column(Modifier.padding(top = LifeTrackerTheme.spacing.md)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    .clearAndSetSemantics {
                        contentDescription = description
                        heading()
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.replaceFirstChar { it.titlecase() },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(total, style = MaterialTheme.typography.titleSmall)
        }
        HorizontalDivider()
    }
}

@Composable
private fun TransactionRow(
    transaction: FinanceTransaction,
    overview: FinanceOverview,
    money: (Money) -> String,
    onClick: (FinanceTransaction) -> Unit,
) {
    val category = overview.categories.firstOrNull { it.id == transaction.categoryId }
    val account = overview.accounts.firstOrNull { it.account.id == transaction.accountId }?.account
    val planned = transaction.isPlannedAt(overview.today)
    val categoryName = category?.displayName().orEmpty()
    val parts =
        listOfNotNull(
            categoryName.takeIf { transaction.note.isNotBlank() },
            account?.displayName()?.takeIf { overview.accounts.size > 1 },
            stringResource(R.string.finance_planned).takeIf { planned },
        )
    TransactionListItem(
        title = transaction.note.ifBlank { categoryName.ifBlank { stringResource(R.string.finance_no_note) } },
        subtitle = parts.joinToString(" · "),
        amount = money(transaction.amount),
        icon = iconFor(category?.iconKey.orEmpty()),
        positive = !transaction.amount.isNegative,
        planned = planned,
        onClick = { onClick(transaction) },
    )
}

@Composable
private fun TransferRow(
    transfer: FinanceTransfer,
    overview: FinanceOverview,
    money: (Money) -> String,
    onClick: (FinanceTransfer) -> Unit,
) {
    val names = overview.accounts.associate { it.account.id to it.account.displayName() }
    TransactionListItem(
        title = transfer.note.ifBlank { stringResource(R.string.finance_transfer_title) },
        subtitle =
            stringResource(
                R.string.finance_transfer_row,
                names[transfer.fromAccountId].orEmpty(),
                names[transfer.toAccountId].orEmpty(),
            ),
        amount = money(transfer.amount),
        icon = Icons.Rounded.SwapHoriz,
        positive = false,
        onClick = { onClick(transfer) },
    )
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

private val EMPTY_MIN_HEIGHT = 280.dp
