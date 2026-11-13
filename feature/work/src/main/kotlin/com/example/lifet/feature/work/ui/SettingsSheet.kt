package com.example.lifet.feature.work.ui

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
import com.example.lifet.core.designsystem.component.LTMultiChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.SettingsDraft
import com.example.lifet.feature.work.rememberWorkFormatters
import java.time.DayOfWeek

/** Objetivo semanal en horas y días laborables. */
@Composable
internal fun SettingsSheet(
    initial: SettingsDraft,
    onDismiss: () -> Unit,
    onSave: (SettingsDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf(initial) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    LTQuickAddSheet(
        title = stringResource(R.string.work_settings_title),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            SettingsFields(draft, submitted) { draft = it }
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.isValid) onSave(draft)
                },
            )
        }
    }
}

@Composable
private fun SettingsFields(
    draft: SettingsDraft,
    submitted: Boolean,
    onChange: (SettingsDraft) -> Unit,
) {
    val formatters = rememberWorkFormatters()
    val invalidTarget = submitted && draft.targetMinutes == null
    TextInput(
        value = draft.hoursText,
        onValueChange = { onChange(draft.copy(hoursText = it.filter(::isHoursChar))) },
        label = stringResource(R.string.work_settings_hours_field),
        maxLength = HOURS_MAX_CHARS,
        supporting = stringResource(R.string.work_settings_hours_hint),
        error = if (invalidTarget) stringResource(R.string.work_issue_target_invalid) else null,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.work_settings_days),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LTMultiChipGroup(
        options = DayOfWeek.entries,
        selected = draft.workingDays,
        label = formatters::dayName,
        onToggle = { onChange(draft.withDayToggled(it)) },
    )
    Text(
        stringResource(R.string.work_settings_days_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (submitted && draft.workingDays.isEmpty()) {
        Text(
            stringResource(R.string.work_issue_no_working_days),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun isHoursChar(char: Char): Boolean = char.isDigit() || char == ',' || char == '.'

private const val HOURS_MAX_CHARS = 6
