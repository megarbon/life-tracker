package com.example.lifet.feature.work.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.work.ShiftIssue
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.feature.work.BreakDraft
import com.example.lifet.feature.work.MINUTES_IN_DAY
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.ShiftDraft
import com.example.lifet.feature.work.WorkEditor
import com.example.lifet.feature.work.blankShiftDraft
import com.example.lifet.feature.work.messageRes
import com.example.lifet.feature.work.rememberWorkFormatters
import com.example.lifet.feature.work.toDraft

/**
 * Hoja de alta y edición de una jornada. Las horas se eligen con selectores; las reglas (salida posterior a la
 * entrada, pausas dentro de la jornada y sin solaparse, 24 h como máximo) se comprueban con el validador del dominio
 * y se enseñan debajo de los campos en cuanto se intenta guardar. El solape con otras jornadas lo decide el dominio
 * al guardar y se cuenta con un mensaje.
 */
@Composable
internal fun ShiftSheet(
    editor: WorkEditor.EditShift,
    projects: List<WorkProject>,
    onDismiss: () -> Unit,
    onSave: (ShiftDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val zone = editor.existing?.zone ?: editor.zone
    var draft by rememberSaveable(editor.existing?.id, editor.newId) {
        mutableStateOf(editor.existing?.toDraft() ?: blankShiftDraft(editor.newId, editor.today))
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val issues = if (submitted) draft.issues(zone, editor.now) else emptySet()
    LTQuickAddSheet(
        title = stringResource(if (editor.existing == null) R.string.work_sheet_add else R.string.work_sheet_edit),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            DatePickerButton(
                date = draft.startDate,
                text = stringResource(R.string.work_field_date, formatters.date(draft.startDate)),
                onDateChange = { draft = draft.copy(startDate = it) },
                maxDate = editor.today,
                modifier = Modifier.fillMaxWidth(),
            )
            TimesRow(draft, formatters::clock) { draft = it }
            if (projects.isNotEmpty()) {
                Text(
                    stringResource(R.string.work_field_project),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProjectPicker(projects, draft.projectId, onSelect = { draft = draft.copy(projectId = it) })
            }
            BreaksSection(draft, formatters::clock) { draft = it }
            TextInput(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                label = stringResource(R.string.work_field_note),
                maxLength = WorkRules.SHIFT_NOTE_MAX_LENGTH,
                maxLines = NOTE_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
            IssuesText(issues)
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.issues(zone, editor.now).isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

private const val NOTE_LINES = 3

@Composable
private fun TimesRow(
    draft: ShiftDraft,
    clock: (Int) -> String,
    onChange: (ShiftDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        TimePickerButton(
            minuteOfDay = draft.startMinute,
            text = stringResource(R.string.work_field_start, clock(draft.startMinute)),
            onChange = { onChange(draft.copy(startMinute = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().semantics { role = Role.Checkbox },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = !draft.hasEnd, onCheckedChange = { onChange(draft.copy(hasEnd = !it)) })
            Text(
                text = stringResource(R.string.work_field_still_open),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
        if (draft.hasEnd) {
            val label =
                if (draft.endsNextDay) R.string.work_field_end_next_day else R.string.work_field_end
            TimePickerButton(
                minuteOfDay = draft.endMinute,
                text = stringResource(label, clock(draft.endMinute)),
                onChange = { onChange(draft.copy(endMinute = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BreaksSection(
    draft: ShiftDraft,
    clock: (Int) -> String,
    onChange: (ShiftDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        Text(
            stringResource(R.string.work_field_breaks),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        draft.breaks.forEachIndexed { index, pause ->
            BreakRow(
                pause = pause,
                clock = clock,
                onChange = { updated ->
                    onChange(
                        draft.copy(
                            breaks =
                                draft.breaks.toMutableList().also {
                                    it[index] =
                                        updated
                                },
                        ),
                    )
                },
                onRemove = { onChange(draft.copy(breaks = draft.breaks.filterIndexed { i, _ -> i != index })) },
            )
        }
        if (draft.breaks.size < WorkRules.MAX_BREAKS) {
            OutlinedButton(
                onClick = { onChange(draft.copy(breaks = draft.breaks + newBreakAfter(draft))) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.work_field_add_break),
                    modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
                )
            }
        }
    }
}

/** Una pausa nueva de 15 minutos a media jornada, cerrada, para que ya sea válida y sólo haya que ajustarla. */
private fun newBreakAfter(draft: ShiftDraft): BreakDraft {
    val start = ((draft.startMinute + BREAK_OFFSET_MINUTES) % MINUTES_IN_DAY)
    return BreakDraft(start, (start + BREAK_MINUTES) % MINUTES_IN_DAY)
}

private const val BREAK_OFFSET_MINUTES = 240
private const val BREAK_MINUTES = 15

@Composable
private fun BreakRow(
    pause: BreakDraft,
    clock: (Int) -> String,
    onChange: (BreakDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
            TimePickerButton(
                minuteOfDay = pause.startMinute,
                text = stringResource(R.string.work_break_start, clock(pause.startMinute)),
                onChange = { onChange(pause.copy(startMinute = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            pause.endMinute?.let { end ->
                TimePickerButton(
                    minuteOfDay = end,
                    text = stringResource(R.string.work_break_end, clock(end)),
                    onChange = { onChange(pause.copy(endMinute = it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.work_field_remove_break))
        }
    }
}

/** Problemas de validación, uno por línea, con el color de error (y siempre con texto: no sólo color). */
@Composable
internal fun IssuesText(issues: Set<ShiftIssue>) {
    if (issues.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        issues.forEach { issue ->
            Text(
                text = stringResource(issue.messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
