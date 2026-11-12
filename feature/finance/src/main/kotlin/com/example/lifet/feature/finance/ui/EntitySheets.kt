package com.example.lifet.feature.finance.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTAmountField
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.util.toEditableText
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.EntryType
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.Frequency
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.feature.finance.AccountDraft
import com.example.lifet.feature.finance.CategoryDraft
import com.example.lifet.feature.finance.DraftIssue
import com.example.lifet.feature.finance.DraftResult
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.RuleDraft
import com.example.lifet.feature.finance.issues
import com.example.lifet.feature.finance.labelRes
import java.util.UUID

/** Alta y edición de una cuenta. La divisa sólo se elige al crearla: los movimientos ya guardados la usan. */
@Composable
internal fun AccountSheet(
    existing: Account?,
    overview: FinanceOverview,
    onDismiss: () -> Unit,
    onSave: (AccountDraft) -> Unit,
) {
    var draft by rememberSaveable(existing?.id) {
        mutableStateOf(
            AccountDraft(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = existing?.name.orEmpty(),
                type = existing?.type ?: AccountType.BANK,
                openingText = existing?.openingBalance?.toEditableText().orEmpty(),
                currencyText = (existing?.currency ?: overview.currency).value,
                archived = existing?.archived ?: false,
            ),
        )
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val issues = if (submitted) draft.validate(existing, 0).issues() else emptySet()
    val title =
        stringResource(
            if (existing ==
                null
            ) {
                R.string.finance_account_sheet_add
            } else {
                R.string.finance_account_sheet_edit
            },
        )
    LTQuickAddSheet(title = title, onDismiss = onDismiss) {
        SheetColumn {
            AccountFields(
                draft = draft,
                canChangeCurrency = existing == null,
                issues = issues,
                onChange = { draft = it },
            )
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate(existing, 0) is DraftResult.Valid) onSave(draft)
                },
            )
        }
    }
}

@Composable
private fun AccountFields(
    draft: AccountDraft,
    canChangeCurrency: Boolean,
    issues: Set<DraftIssue>,
    onChange: (AccountDraft) -> Unit,
) {
    val currency = runCatching { CurrencyCode(draft.currencyText.trim().uppercase()) }.getOrNull()
    NameField(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        label = stringResource(R.string.finance_field_name),
        error = issueText(issues, DraftIssue.NAME_MISSING),
    )
    val typeLabels = AccountType.entries.associateWith { stringResource(it.labelRes()) }
    LTChipGroup(
        options = AccountType.entries,
        selected = draft.type,
        label = { typeLabels[it].orEmpty() },
        onSelect = { onChange(draft.copy(type = it)) },
    )
    NameField(
        value = draft.currencyText,
        onValueChange = { if (canChangeCurrency) onChange(draft.copy(currencyText = it.take(CURRENCY_LENGTH))) },
        label = stringResource(R.string.finance_field_currency),
        error = issueText(issues, DraftIssue.CURRENCY_INVALID),
    )
    LTAmountField(
        text = draft.openingText,
        onTextChange = { onChange(draft.copy(openingText = it)) },
        currency = currency ?: CurrencyCode.EUR,
        label = stringResource(R.string.finance_field_opening),
        modifier = Modifier.fillMaxWidth(),
        errorMessage = issueText(issues, DraftIssue.AMOUNT_INVALID),
    )
    ToggleRow(stringResource(R.string.finance_archived), draft.archived) { onChange(draft.copy(archived = it)) }
}

/** Alta y edición de una categoría. El tipo (gasto/ingreso) se fija al crearla. */
@Composable
internal fun CategorySheet(
    existing: FinanceCategory?,
    onDismiss: () -> Unit,
    onSave: (CategoryDraft) -> Unit,
) {
    var draft by rememberSaveable(existing?.id) {
        mutableStateOf(
            CategoryDraft(
                id = existing?.id ?: UUID.randomUUID().toString(),
                kind = existing?.kind ?: CategoryKind.EXPENSE,
                name = existing?.customName.orEmpty(),
                archived = existing?.archived ?: false,
            ),
        )
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val issues = if (submitted) draft.validate(existing, 0).issues() else emptySet()
    val title =
        stringResource(
            if (existing ==
                null
            ) {
                R.string.finance_category_sheet_add
            } else {
                R.string.finance_category_sheet_edit
            },
        )
    LTQuickAddSheet(title = title, onDismiss = onDismiss) {
        SheetColumn {
            if (existing == null) KindPicker(draft.kind) { draft = draft.copy(kind = it) }
            NameField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = stringResource(R.string.finance_field_name),
                error = issueText(issues, DraftIssue.NAME_MISSING),
                supporting =
                    if (existing?.defaultCategory !=
                        null
                    ) {
                        stringResource(R.string.finance_category_default_hint)
                    } else {
                        null
                    },
            )
            ToggleRow(stringResource(R.string.finance_archived), draft.archived) { draft = draft.copy(archived = it) }
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate(existing, 0) is DraftResult.Valid) onSave(draft)
                },
            )
        }
    }
}

@Composable
private fun KindPicker(
    selected: CategoryKind,
    onSelect: (CategoryKind) -> Unit,
) {
    val labels =
        mapOf(
            CategoryKind.EXPENSE to stringResource(R.string.finance_entry_expense),
            CategoryKind.INCOME to stringResource(R.string.finance_entry_income),
        )
    LTChipGroup(
        options = CategoryKind.entries,
        selected = selected,
        label = { labels[it].orEmpty() },
        onSelect = onSelect,
    )
}

/** Alta y edición de un recurrente (nómina, alquiler, suscripción). */
@Composable
internal fun RuleSheet(
    existing: RecurringRule?,
    overview: FinanceOverview,
    onDismiss: () -> Unit,
    onSave: (RuleDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val accounts = overview.accounts.map { it.account }.filter { !it.archived || it.id == existing?.accountId }
    var draft by rememberSaveable(existing?.id) { mutableStateOf(initialRuleDraft(existing, accounts, overview)) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val account = accounts.firstOrNull { it.id == draft.accountId }
    val issues = if (submitted) draft.validate(account, existing).issues() else emptySet()
    val title =
        stringResource(
            if (existing ==
                null
            ) {
                R.string.finance_rule_sheet_add
            } else {
                R.string.finance_rule_sheet_edit
            },
        )
    LTQuickAddSheet(title = title, onDismiss = onDismiss) {
        SheetColumn {
            RuleFields(draft, overview, accounts, account?.currency ?: overview.currency, issues) { draft = it }
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate(account, existing) is DraftResult.Valid) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun RuleFields(
    draft: RuleDraft,
    overview: FinanceOverview,
    accounts: List<Account>,
    currency: CurrencyCode,
    issues: Set<DraftIssue>,
    onChange: (RuleDraft) -> Unit,
) {
    val types = EntryType.entries.filter { it != EntryType.REFUND }
    val typeLabels = types.associateWith { stringResource(it.labelRes()) }
    LTChipGroup(
        options = types,
        selected = draft.type,
        label = { typeLabels[it].orEmpty() },
        onSelect = { type ->
            val keep = overview.categories.firstOrNull { it.id == draft.categoryId }?.kind == type.categoryKind
            onChange(draft.copy(type = type, categoryId = if (keep) draft.categoryId else null))
        },
    )
    NameField(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        label = stringResource(R.string.finance_field_name),
        error = issueText(issues, DraftIssue.NAME_MISSING),
    )
    LTAmountField(
        text = draft.amountText,
        onTextChange = { onChange(draft.copy(amountText = it)) },
        currency = currency,
        label = stringResource(R.string.finance_field_amount),
        modifier = Modifier.fillMaxWidth(),
        errorMessage = issueText(issues, DraftIssue.AMOUNT_MISSING, DraftIssue.AMOUNT_INVALID),
    )
    CategoryPicker(
        categories = overview.categories.filter { it.kind == draft.type.categoryKind && !it.archived },
        selectedId = draft.categoryId,
        onSelect = { onChange(draft.copy(categoryId = it)) },
        error = issueText(issues, DraftIssue.CATEGORY_MISSING),
    )
    if (accounts.size > 1) AccountPicker(accounts, draft.accountId) { onChange(draft.copy(accountId = it)) }
    val frequencyLabels = Frequency.entries.associateWith { stringResource(it.labelRes()) }
    LTChipGroup(
        options = Frequency.entries,
        selected = draft.frequency,
        label = { frequencyLabels[it].orEmpty() },
        onSelect = { onChange(draft.copy(frequency = it)) },
    )
    DateField(draft.startDate, { onChange(draft.copy(startDate = it)) }, stringResource(R.string.finance_field_start))
    ToggleRow(stringResource(R.string.finance_rule_active), draft.active) { onChange(draft.copy(active = it)) }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun initialRuleDraft(
    existing: RecurringRule?,
    accounts: List<Account>,
    overview: FinanceOverview,
): RuleDraft {
    val category = overview.categories.firstOrNull { it.id == existing?.categoryId }
    return RuleDraft(
        id = existing?.id ?: UUID.randomUUID().toString(),
        type = existing?.let { if (it.amount.isNegative) EntryType.EXPENSE else EntryType.INCOME } ?: EntryType.EXPENSE,
        name = existing?.name.orEmpty(),
        amountText = existing?.amount?.toEditableText().orEmpty(),
        accountId = existing?.accountId ?: accounts.firstOrNull { !it.archived }?.id,
        categoryId = category?.id,
        frequency = existing?.frequency ?: Frequency.MONTHLY,
        startDate = existing?.startDate ?: overview.today,
        active = existing?.active ?: true,
    )
}

private const val CURRENCY_LENGTH = 3
