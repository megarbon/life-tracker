package com.example.lifet.feature.work.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.work.WorkOverview
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.core.model.work.WorkWeekReport
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.WorkAction
import com.example.lifet.feature.work.rememberDurationFormat
import com.example.lifet.feature.work.rememberWorkFormatters
import java.time.ZoneId

/**
 * Hoy: el héroe con el cronómetro, el aviso de jornadas sin cerrar, las jornadas de hoy, la nota del día y las dos
 * gráficas de la semana. Los datos de la semana mostrada llegan en [week]; aquí siempre es la actual.
 */
@Composable
internal fun TodayTab(
    overview: WorkOverview,
    week: WorkWeekReport,
    zone: ZoneId,
    contentPadding: PaddingValues,
    onAction: (WorkAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val durations = rememberDurationFormat()
    val projects = overview.projects.associate { it.project.id to it.project }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "hero", contentType = "hero") {
            TodayHero(
                summary = overview.summary,
                zone = zone,
                onClockIn = { onAction(WorkAction.ClockIn(null)) },
                onBreak = { onAction(WorkAction.StartBreak) },
                onResume = { onAction(WorkAction.EndBreak) },
                onClockOut = { onAction(WorkAction.ClockOut) },
            )
        }
        items(overview.forgottenShifts, key = { "forgotten-${it.id}" }, contentType = { "forgotten" }) { shift ->
            ForgottenShiftCard(shift, onClose = { onAction(WorkAction.OpenClose(shift)) })
        }
        item(key = "shifts-header", contentType = "header") {
            LTSectionHeader(
                title = stringResource(R.string.work_today_shifts),
                actionLabel = stringResource(R.string.work_add_shift),
                onAction = { onAction(WorkAction.OpenShift()) },
            )
        }
        if (overview.todayShifts.isEmpty()) {
            item(key = "shifts-empty", contentType = "text") {
                Text(
                    text = stringResource(R.string.work_today_no_shifts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(overview.todayShifts, key = { "shift-${it.id}" }, contentType = { "shift" }) { shift ->
            ShiftListItem(
                shift = shift,
                project = shift.projectId?.let(projects::get),
                snapshotNow = overview.now,
                formatters = formatters,
                durations = durations,
                onClick = { onAction(WorkAction.OpenShift(shift)) },
            )
        }
        item(key = "note", contentType = "note") {
            DayNoteCard(
                text = overview.todayNote,
                onEdit = { onAction(WorkAction.OpenNote(overview.today, overview.todayNote)) },
            )
        }
        item(key = "chart-hours", contentType = "chart") { HoursChart(week) }
        item(key = "chart-balance", contentType = "chart") { BalanceChart(week) }
    }
}

/** Una jornada que se quedó abierta: invita, sin culpar, a indicar a qué hora terminó para contar sus horas. */
@Composable
private fun ForgottenShiftCard(
    shift: WorkShift,
    onClose: () -> Unit,
) {
    val formatters = rememberWorkFormatters()
    val date = formatters.date(shift.startDate)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.work_forgotten_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.work_forgotten_message, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.work_forgotten_action))
            }
        }
    }
}

/** Nota libre del día; un toque la edita. */
@Composable
private fun DayNoteCard(
    text: String,
    onEdit: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(LifeTrackerTheme.spacing.md)
                    .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.work_note_title),
                style = MaterialTheme.typography.titleSmall,
                color = muted,
            )
            Text(
                text = text.ifBlank { stringResource(R.string.work_note_empty) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (text.isBlank()) muted else MaterialTheme.colorScheme.onSurface,
                maxLines = NOTE_PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.work_note_edit))
            }
        }
    }
}

private const val NOTE_PREVIEW_LINES = 4
