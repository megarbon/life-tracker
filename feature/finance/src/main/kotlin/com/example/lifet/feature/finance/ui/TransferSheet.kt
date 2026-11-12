package com.example.lifet.feature.finance.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTAmountField
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.util.toEditableText
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.feature.finance.DraftIssue
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.TransferDraft
import com.example.lifet.feature.finance.displayName
import com.example.lifet.feature.finance.issues
import java.util.UUID

/** Alta y edición de una transferencia entre dos cuentas de la misma divisa. */
@Composable
internal fun TransferSheet(
    overview: FinanceOverview,
    existing: FinanceTransfer?,
    onDismiss: () -> Unit,
    onSave: (TransferDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val accounts = overview.accounts.map { it.account }.filter { !it.archived }
    var draft by rememberSaveable(existing?.id) {
        mutableStateOf(
            TransferDraft(
                id = existing?.id ?: UUID.randomUUID().toString(),
                fromAccountId = existing?.fromAccountId ?: accounts.getOrNull(0)?.id,
                toAccountId = existing?.toAccountId ?: accounts.getOrNull(1)?.id,
                amountText = existing?.amount?.toEditableText().orEmpty(),
                date = existing?.date ?: overview.today,
                note = existing?.note.orEmpty(),
            ),
        )
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val from = accounts.firstOrNull { it.id == draft.fromAccountId }
    val to = accounts.firstOrNull { it.id == draft.toAccountId }
    val issues = if (submitted) draft.issues(from, to) else emptySet()
    val title =
        stringResource(
            if (existing ==
                null
            ) {
                R.string.finance_transfer_sheet_add
            } else {
                R.string.finance_transfer_sheet_edit
            },
        )
    LTQuickAddSheet(title = title, onDismiss = onDismiss) {
        SheetColumn {
            Text(stringResource(R.string.finance_field_from), style = MaterialTheme.typography.labelLarge)
            AccountPicker(accounts, draft.fromAccountId) { draft = draft.copy(fromAccountId = it) }
            Text(stringResource(R.string.finance_field_to), style = MaterialTheme.typography.labelLarge)
            AccountPicker(accounts, draft.toAccountId) { draft = draft.copy(toAccountId = it) }
            issueText(issues, DraftIssue.ACCOUNT_MISSING, DraftIssue.SAME_ACCOUNT, DraftIssue.CURRENCY_MISMATCH)
                ?.let { MutedError(it) }
            LTAmountField(
                text = draft.amountText,
                onTextChange = { draft = draft.copy(amountText = it) },
                currency = from?.currency ?: overview.currency,
                label = stringResource(R.string.finance_field_amount),
                modifier = Modifier.fillMaxWidth(),
                errorMessage = issueText(issues, DraftIssue.AMOUNT_MISSING, DraftIssue.AMOUNT_INVALID),
            )
            DateField(draft.date, { draft = draft.copy(date = it) }, stringResource(R.string.finance_field_date))
            NameField(draft.note, { draft = draft.copy(note = it) }, stringResource(R.string.finance_field_note))
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.issues(from, to).isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

/** Fijar o quitar el límite mensual de una categoría de gasto. */
@Composable
internal fun BudgetSheet(
    category: FinanceCategory,
    overview: FinanceOverview,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val current = overview.budgets.firstOrNull { it.categoryId == category.id }?.limit
    var text by rememberSaveable(category.id) { mutableStateOf(current?.toEditableText().orEmpty()) }
    LTQuickAddSheet(
        title = stringResource(R.string.finance_budget_sheet_title, category.displayName()),
        onDismiss = onDismiss,
    ) {
        SheetColumn {
            LTAmountField(
                text = text,
                onTextChange = { text = it },
                currency = current?.currency ?: overview.currency,
                label = stringResource(R.string.finance_budget_field),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.finance_budget_hint_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SheetActions(onSave = { onSave(text) })
        }
    }
}
