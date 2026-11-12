package com.example.lifet.feature.finance.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.feature.finance.DraftIssue
import com.example.lifet.feature.finance.R
import com.example.lifet.feature.finance.mediumDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Texto de error de un problema de formulario, o `null` si no aplica a este campo. */
@Composable
internal fun issueText(
    issues: Set<DraftIssue>,
    vararg relevant: DraftIssue,
): String? = relevant.firstOrNull { it in issues }?.let { stringResource(it.messageRes()) }

@StringRes
private fun DraftIssue.messageRes(): Int =
    when (this) {
        DraftIssue.AMOUNT_MISSING -> R.string.finance_issue_amount_missing
        DraftIssue.AMOUNT_INVALID -> R.string.finance_issue_amount_invalid
        DraftIssue.ACCOUNT_MISSING -> R.string.finance_issue_account_missing
        DraftIssue.CATEGORY_MISSING -> R.string.finance_issue_category_missing
        DraftIssue.SAME_ACCOUNT -> R.string.finance_issue_same_account
        DraftIssue.CURRENCY_MISMATCH -> R.string.finance_issue_currency_mismatch
        DraftIssue.CURRENCY_INVALID -> R.string.finance_issue_currency_invalid
        DraftIssue.NAME_MISSING -> R.string.finance_issue_name_missing
    }

@Composable
internal fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

/** Botón que muestra la fecha y abre un selector de fecha de Material. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
    ) {
        Text("$label: ${mediumDate(date)}", maxLines = 2)
    }
    if (open) {
        // El selector trabaja en milisegundos UTC a medianoche: se evita la zona local para no correr el día.
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        open = false
                    },
                ) { Text(stringResource(R.string.finance_date_ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { open = false },
                ) { Text(stringResource(com.example.lifet.core.designsystem.R.string.lt_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

/** Fila de acciones de una hoja: borrar (si se edita) y guardar. */
@Composable
internal fun SheetActions(
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        if (onDelete != null) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) { Text(stringResource(R.string.finance_delete), color = MaterialTheme.colorScheme.error) }
        }
        Button(onClick = onSave, modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.finance_save))
        }
    }
}

@Composable
internal fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) { content() }
}

@Composable
internal fun MutedError(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
