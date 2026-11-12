package com.example.lifet.feature.finance.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTAmountField
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.util.toEditableText
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.EntryType
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.feature.finance.DraftIssue
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.TransactionDraft
import com.example.lifet.feature.finance.displayName
import com.example.lifet.feature.finance.labelRes
import java.time.LocalDate
import java.util.UUID

/**
 * Hoja de alta y edición de un movimiento. Los movimientos recientes aparecen como chips: un toque rellena
 * importe, categoría y nota para repetir un gasto habitual en menos de cinco segundos.
 */
@Composable
internal fun TransactionSheet(
    overview: FinanceOverview,
    existing: FinanceTransaction?,
    template: FinanceTransaction?,
    onDismiss: () -> Unit,
    onSave: (TransactionDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val accounts = overview.accounts.map { it.account }.filter { !it.archived || it.id == existing?.accountId }
    val categories = overview.categories
    val seed = existing ?: template
    var draft by rememberSaveable(existing?.id, template?.id) {
        mutableStateOf(initialDraft(existing, seed, accounts, categories, overview.today))
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val account = accounts.firstOrNull { it.id == draft.accountId }
    val issues = if (submitted) draft.issues(account) else emptySet()
    val templates = overview.recentTemplates
    val templateLabels = remember(templates) { templates.associateBy { it.note } }

    LTQuickAddSheet(
        title =
            stringResource(if (existing == null) R.string.finance_sheet_add else R.string.finance_sheet_edit),
        onDismiss = onDismiss,
        recents = if (existing == null) templates.map { it.note } else emptyList(),
        onRecentPick = { note ->
            templateLabels[note]?.let { draft = initialDraft(null, it, accounts, categories, overview.today) }
        },
    ) {
        SheetColumn {
            TransactionFields(
                draft = draft,
                categories = categories,
                accounts = accounts,
                currency = account?.currency ?: overview.currency,
                issues = issues,
                onChange = { draft = it },
            )
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.issues(account).isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun TransactionFields(
    draft: TransactionDraft,
    categories: List<FinanceCategory>,
    accounts: List<Account>,
    currency: CurrencyCode,
    issues: Set<DraftIssue>,
    onChange: (TransactionDraft) -> Unit,
) {
    EntryTypePicker(draft.type) { type ->
        // Cambiar entre gasto/devolución e ingreso invalida la categoría elegida.
        val kind = type.categoryKind
        val keep = categories.firstOrNull { it.id == draft.categoryId }?.kind == kind
        onChange(draft.copy(type = type, categoryId = if (keep) draft.categoryId else null))
    }
    LTAmountField(
        text = draft.amountText,
        onTextChange = { onChange(draft.copy(amountText = it)) },
        currency = currency,
        label = stringResource(R.string.finance_field_amount),
        modifier = Modifier.fillMaxWidth(),
        errorMessage = issueText(issues, DraftIssue.AMOUNT_MISSING, DraftIssue.AMOUNT_INVALID),
    )
    CategoryPicker(
        categories =
            categories.filter {
                it.kind == draft.type.categoryKind &&
                    (!it.archived || it.id == draft.categoryId)
            },
        selectedId = draft.categoryId,
        onSelect = { onChange(draft.copy(categoryId = it)) },
        error = issueText(issues, DraftIssue.CATEGORY_MISSING),
    )
    if (accounts.size > 1) AccountPicker(accounts, draft.accountId) { onChange(draft.copy(accountId = it)) }
    DateField(
        date = draft.date,
        onDateChange = { onChange(draft.copy(date = it)) },
        label = stringResource(R.string.finance_field_date),
    )
    OutlinedTextField(
        value = draft.note,
        onValueChange = { onChange(draft.copy(note = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.finance_field_note)) },
        minLines = 1,
        maxLines = NOTE_MAX_LINES,
    )
}

@Composable
private fun EntryTypePicker(
    selected: EntryType,
    onSelect: (EntryType) -> Unit,
) {
    val labels = EntryType.entries.associateWith { stringResource(it.labelRes()) }
    LTChipGroup(
        options = EntryType.entries,
        selected = selected,
        label = { labels[it].orEmpty() },
        onSelect = onSelect,
    )
}

@Composable
internal fun CategoryPicker(
    categories: List<FinanceCategory>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    error: String?,
) {
    val names = categories.associate { it.id to it.displayName() }
    LTChipGroup(
        options = categories.map { it.id },
        selected = selectedId,
        label = { names[it].orEmpty() },
        onSelect = onSelect,
    )
    if (error != null) MutedError(error)
}

@Composable
internal fun AccountPicker(
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val names = accounts.associate { it.id to it.displayName() }
    LTChipGroup(
        options = accounts.map { it.id },
        selected = selectedId,
        label = { names[it].orEmpty() },
        onSelect = onSelect,
    )
}

private fun initialDraft(
    existing: FinanceTransaction?,
    seed: FinanceTransaction?,
    accounts: List<Account>,
    categories: List<FinanceCategory>,
    today: LocalDate,
): TransactionDraft {
    val category = categories.firstOrNull { it.id == seed?.categoryId }
    val account = accounts.firstOrNull { it.id == seed?.accountId } ?: accounts.firstOrNull { !it.archived }
    return TransactionDraft(
        id = existing?.id ?: UUID.randomUUID().toString(),
        type = seed?.let { EntryType.of(it.amount, category?.kind) } ?: EntryType.EXPENSE,
        amountText = seed?.amount?.toEditableText().orEmpty(),
        accountId = account?.id,
        categoryId = category?.id,
        date = existing?.date ?: today,
        note = seed?.note.orEmpty(),
    )
}

private const val NOTE_MAX_LINES = 3
