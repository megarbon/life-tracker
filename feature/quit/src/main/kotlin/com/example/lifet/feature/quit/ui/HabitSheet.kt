package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.util.sanitizeAmountInput
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitRules
import com.example.lifet.feature.quit.DraftIssue
import com.example.lifet.feature.quit.HabitDraft
import com.example.lifet.feature.quit.QuitEditor
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.blankHabitDraft
import com.example.lifet.feature.quit.effectiveMode
import com.example.lifet.feature.quit.issues
import com.example.lifet.feature.quit.labelRes
import com.example.lifet.feature.quit.toDraft
import com.example.lifet.feature.quit.validate
import com.example.lifet.feature.quit.withKind

private const val NAME_MAX_LINES = 1

/**
 * Hoja de alta y edición de un hábito: qué se deja, desde cuándo, cuánto se consumía y cuánto costaba. El alcohol
 * admite modo reducción con un límite semanal. El texto del precio es de la UI y se convierte a `Money` al validar:
 * nunca pasa por `Double`.
 */
@Composable
internal fun HabitSheet(
    editor: QuitEditor.Habit,
    onDismiss: () -> Unit,
    onSave: (HabitDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val now = editor.now
    val zone = editor.existing?.zone ?: editor.zone
    var draft by rememberSaveable(editor.newId, editor.existing?.id) { mutableStateOf(initialDraft(editor)) }
    var submitted by rememberSaveable(editor.newId) { mutableStateOf(false) }
    val issues = if (submitted) draft.validate(zone, now).issues() else emptySet()
    val isEdit = editor.existing != null

    LTQuickAddSheet(
        title = stringResource(if (isEdit) R.string.quit_habit_sheet_edit else R.string.quit_habit_sheet_add),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            HabitFields(draft, issues, isEdit) { draft = it }
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate(zone, now).issues().isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

private fun initialDraft(editor: QuitEditor.Habit): HabitDraft =
    editor.existing?.toDraft()
        ?: blankHabitDraft(
            id = editor.newId,
            kind = editor.kind,
            today = editor.today,
            nowMinute = editor.nowMinute,
            currency = runCatching { CurrencyCode(editor.currency) }.getOrDefault(CurrencyCode.EUR),
        )

@Composable
private fun HabitFields(
    draft: HabitDraft,
    issues: Set<DraftIssue>,
    isEdit: Boolean,
    onChange: (HabitDraft) -> Unit,
) {
    val tobacco = draft.kind == HabitKind.TOBACCO
    KindAndModePickers(draft, isEdit, onChange)
    TextInput(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
        label = stringResource(R.string.quit_field_name),
        maxLength = QuitRules.NAME_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth(),
        maxLines = NAME_MAX_LINES,
    )
    DateField(
        date = draft.date,
        onDateChange = { onChange(draft.copy(date = it)) },
        labelRes = R.string.quit_field_start_date,
        modifier = Modifier.fillMaxWidth(),
    )
    TimeField(
        minuteOfDay = draft.startMinute,
        onChange = { onChange(draft.copy(startMinute = it)) },
        labelRes = R.string.quit_field_start_time,
        modifier = Modifier.fillMaxWidth(),
    )
    issueText(issues, DraftIssue.START_IN_FUTURE)?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    BaselineFields(draft, issues, tobacco, onChange)
    PriceFields(draft, issues, tobacco, onChange)
    if (draft.effectiveMode() == HabitMode.REDUCE) {
        NumberField(
            value = draft.limitText,
            onValueChange = { onChange(draft.copy(limitText = it)) },
            label = stringResource(R.string.quit_field_weekly_limit),
            modifier = Modifier.fillMaxWidth(),
            error = issueText(issues, DraftIssue.LIMIT_MISSING, DraftIssue.LIMIT_INVALID),
            imeAction = ImeAction.Done,
        )
    }
}

/** Qué se deja (sólo al crear) y, para el alcohol, si se deja del todo o se reduce con un límite semanal. */
@Composable
private fun KindAndModePickers(
    draft: HabitDraft,
    isEdit: Boolean,
    onChange: (HabitDraft) -> Unit,
) {
    if (!isEdit) {
        val kinds = HabitKind.entries.associateWith { stringResource(it.labelRes) }
        LTChipGroup(
            options = HabitKind.entries,
            selected = draft.kind,
            label = { kinds.getValue(it) },
            onSelect = { onChange(draft.withKind(it)) },
        )
    }
    if (draft.kind == HabitKind.ALCOHOL) {
        val modes = HabitMode.entries.associateWith { stringResource(it.labelRes) }
        LTChipGroup(
            options = HabitMode.entries,
            selected = draft.mode,
            label = { modes.getValue(it) },
            onSelect = { onChange(draft.copy(mode = it)) },
        )
        Text(
            text = stringResource(R.string.quit_mode_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BaselineFields(
    draft: HabitDraft,
    issues: Set<DraftIssue>,
    tobacco: Boolean,
    onChange: (HabitDraft) -> Unit,
) {
    val periods = BaselinePeriod.entries.associateWith { stringResource(it.labelRes) }
    LTChipGroup(
        options = BaselinePeriod.entries,
        selected = draft.period,
        label = { periods.getValue(it) },
        onSelect = { onChange(draft.copy(period = it)) },
    )
    val labelRes =
        when {
            tobacco && draft.period == BaselinePeriod.DAY -> R.string.quit_field_baseline_day_tobacco
            tobacco -> R.string.quit_field_baseline_week_tobacco
            draft.period == BaselinePeriod.DAY -> R.string.quit_field_baseline_day_alcohol
            else -> R.string.quit_field_baseline_week_alcohol
        }
    NumberField(
        value = draft.baselineText,
        onValueChange = { onChange(draft.copy(baselineText = it)) },
        label = stringResource(labelRes),
        modifier = Modifier.fillMaxWidth(),
        error = issueText(issues, DraftIssue.BASELINE_MISSING, DraftIssue.BASELINE_INVALID),
    )
    if (tobacco) {
        NumberField(
            value = draft.packSizeText,
            onValueChange = { onChange(draft.copy(packSizeText = it)) },
            label = stringResource(R.string.quit_field_pack_size),
            modifier = Modifier.fillMaxWidth(),
            error = issueText(issues, DraftIssue.PACK_SIZE_MISSING, DraftIssue.PACK_SIZE_INVALID),
        )
    }
}

@Composable
private fun PriceFields(
    draft: HabitDraft,
    issues: Set<DraftIssue>,
    tobacco: Boolean,
    onChange: (HabitDraft) -> Unit,
) {
    val currency = runCatching { CurrencyCode(draft.currency.trim().uppercase()) }.getOrNull()
    OutlinedTextField(
        value = draft.priceText,
        onValueChange = { raw ->
            val sanitized =
                currency?.let { sanitizeAmountInput(raw, it) } ?: raw.filter { it.isDigit() || it == ',' || it == '.' }
            onChange(draft.copy(priceText = sanitized))
        },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                stringResource(if (tobacco) R.string.quit_field_pack_price else R.string.quit_field_drink_price),
            )
        },
        suffix = { Text(draft.currency.uppercase()) },
        singleLine = true,
        isError = issueText(issues, DraftIssue.PRICE_MISSING, DraftIssue.PRICE_INVALID) != null,
        supportingText = issueText(issues, DraftIssue.PRICE_MISSING, DraftIssue.PRICE_INVALID)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = draft.currency,
        onValueChange = {
            onChange(
                draft.copy(
                    currency =
                        it
                            .filter { char ->
                                char.isLetter()
                            }.take(CURRENCY_LENGTH)
                            .uppercase(),
                ),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.quit_field_currency)) },
        singleLine = true,
        isError = issueText(issues, DraftIssue.CURRENCY_INVALID) != null,
        supportingText = issueText(issues, DraftIssue.CURRENCY_INVALID)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

private const val CURRENCY_LENGTH = 3
