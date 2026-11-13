package com.example.lifet.feature.work.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.feature.work.ProjectDraft
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.SettingsDraft
import com.example.lifet.feature.work.WorkEditor
import com.example.lifet.feature.work.color
import com.example.lifet.feature.work.labelRes
import com.example.lifet.feature.work.minuteOfDay
import com.example.lifet.feature.work.rememberWorkFormatters
import java.time.LocalDate
import java.time.ZoneId

/**
 * Indicar a posteriori la hora de salida de una jornada que se quedó abierta. Se propone la hora de entrada más ocho
 * horas, que es lo más probable; el usuario la ajusta. El dominio rechaza una salida anterior a la entrada, futura o
 * que pise a otra jornada, y la pantalla lo cuenta con un mensaje.
 */
@Composable
internal fun CloseShiftSheet(
    editor: WorkEditor.CloseShift,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Int, ZoneId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val shift = editor.shift
    val suggestion = shift.start.plusSeconds(SUGGESTED_SHIFT_SECONDS).coerceAtMost(editor.now)
    var exitDate by rememberSaveable(shift.id) { mutableStateOf(suggestion.atZone(shift.zone).toLocalDate()) }
    var exitMinute by rememberSaveable(shift.id) { mutableStateOf(suggestion.minuteOfDay(shift.zone)) }
    LTQuickAddSheet(
        title = stringResource(R.string.work_close_title),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            Text(
                text =
                    stringResource(
                        R.string.work_close_message,
                        formatters.date(shift.startDate),
                        formatters.clock(shift.start, shift.zone),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DatePickerButton(
                date = exitDate,
                text = stringResource(R.string.work_close_exit_date, formatters.date(exitDate)),
                onDateChange = { exitDate = it },
                maxDate = editor.now.atZone(shift.zone).toLocalDate(),
                modifier = Modifier.fillMaxWidth(),
            )
            TimePickerButton(
                minuteOfDay = exitMinute,
                text = stringResource(R.string.work_close_exit_time, formatters.clock(exitMinute)),
                onChange = { exitMinute = it },
                modifier = Modifier.fillMaxWidth(),
            )
            SheetActions(
                onSave = { onSave(exitDate, exitMinute, shift.zone) },
                saveLabel = stringResource(R.string.work_close_save),
            )
        }
    }
}

private const val SUGGESTED_SHIFT_SECONDS = 8 * 3_600L

/** Alta y edición de un proyecto: nombre y color de una paleta cerrada. */
@Composable
internal fun ProjectSheet(
    editor: WorkEditor.EditProject,
    onDismiss: () -> Unit,
    onSave: (ProjectDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val existing = editor.existing
    var draft by rememberSaveable(existing?.id) {
        mutableStateOf(ProjectDraft(existing?.id, existing?.name.orEmpty(), existing?.color ?: ProjectColor.BLUE))
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    LTQuickAddSheet(
        title =
            stringResource(
                if (existing ==
                    null
                ) {
                    R.string.work_project_sheet_add
                } else {
                    R.string.work_project_sheet_edit
                },
            ),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            TextInput(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = stringResource(R.string.work_project_name_field),
                maxLength = WorkRules.PROJECT_NAME_MAX_LENGTH,
                error = if (submitted && !draft.isValid) stringResource(R.string.work_issue_name_missing) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.work_project_color_field),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ColorPalette(draft.color) { draft = draft.copy(color = it) }
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.isValid) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

private val SWATCH_SIZE = 40.dp
private val SWATCH_BORDER = 3.dp

/** Paleta de colores. Cada muestra es un objetivo de 48 dp con su nombre para TalkBack; la elegida lleva un tic. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(
    selected: ProjectColor,
    onSelect: (ProjectColor) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        ProjectColor.entries.forEach { option ->
            val name = stringResource(option.labelRes)
            val isSelected = option == selected
            val description = if (isSelected) stringResource(R.string.work_project_color_selected, name) else name
            Box(
                modifier =
                    Modifier
                        .size(MinTouchTarget)
                        .clip(CircleShape)
                        .clickable { onSelect(option) }
                        .clearAndSetSemantics {
                            contentDescription = description
                            role = Role.RadioButton
                            this.selected = isSelected
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(SWATCH_SIZE)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                if (isSelected) SWATCH_BORDER else 0.dp,
                                MaterialTheme.colorScheme.onSurface,
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }
}

/** Nota libre del día. */
@Composable
internal fun NoteSheet(
    editor: WorkEditor.EditNote,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    var text by rememberSaveable(editor.date) { mutableStateOf(editor.text) }
    LTQuickAddSheet(
        title = stringResource(R.string.work_note_sheet_title, formatters.date(editor.date)),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            TextInput(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.work_note_field),
                maxLength = WorkRules.DAY_NOTE_MAX_LENGTH,
                maxLines = NOTE_MAX_LINES,
                modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
            )
            SheetActions(onSave = { onSave(text) })
        }
    }
}

private const val NOTE_MAX_LINES = 8
