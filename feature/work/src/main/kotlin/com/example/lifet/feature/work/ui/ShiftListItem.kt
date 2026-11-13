package com.example.lifet.feature.work.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.feature.work.DurationFormat
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.WorkFormatters
import com.example.lifet.feature.work.color
import com.example.lifet.feature.work.rememberLiveNow
import java.time.Instant

/**
 * Fila de una jornada: horas de entrada y salida, tiempo trabajado con las pausas descontadas, el proyecto y, a la
 * derecha, su estado. Una jornada abierta dice "En curso" (o "Sin cerrar" si es de otro día); una que cruza la
 * medianoche lo indica con texto, no sólo con la hora.
 */
@Composable
internal fun ShiftListItem(
    shift: WorkShift,
    project: WorkProject?,
    snapshotNow: Instant,
    formatters: WorkFormatters,
    durations: DurationFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Una jornada en marcha avanza sola; las cerradas, en pausa u olvidadas se quedan con el instante de la emisión.
    val ticking = shift.isOpen && !shift.isOnBreak && !shift.isForgotten(snapshotNow)
    val liveNow by rememberLiveNow(enabled = ticking)
    val now = if (ticking) liveNow else snapshotNow
    val range = shiftRange(shift, now, formatters)
    val worked = durations.format(shift.netSecondsAt(now))
    val breaks = shift.breakSecondsAt(now)
    val subtitle =
        if (breaks > 0L) {
            stringResource(R.string.work_shift_with_breaks, worked, durations.format(breaks))
        } else {
            stringResource(R.string.work_shift_worked, worked)
        }
    val status = shiftStatus(shift, now)
    val midnight = stringResource(R.string.work_shift_midnight).takeIf { shift.crossesMidnightAt(now) }
    val description = listOfNotNull(range, subtitle, project?.name, status, midnight).joinToString(". ")
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        ColorDot(project?.color?.color ?: MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.weight(1f)) {
            Text(range, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            project?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            midnight?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        status?.let { Text(it, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
    }
}

@Composable
private fun shiftRange(
    shift: WorkShift,
    now: Instant,
    formatters: WorkFormatters,
): String {
    val start = formatters.clock(shift.start, shift.zone)
    val end = shift.end
    return when {
        end == null -> stringResource(R.string.work_shift_range, start, stringResource(R.string.work_shift_open))

        shift.crossesMidnightAt(
            now,
        ) -> stringResource(R.string.work_shift_next_day, start, formatters.clock(end, shift.zone))

        else -> stringResource(R.string.work_shift_range, start, formatters.clock(end, shift.zone))
    }
}

@Composable
private fun shiftStatus(
    shift: WorkShift,
    now: Instant,
): String? =
    when {
        !shift.isOpen -> null
        shift.isForgotten(now) -> stringResource(R.string.work_shift_open_forgotten)
        else -> stringResource(R.string.work_shift_open)
    }
