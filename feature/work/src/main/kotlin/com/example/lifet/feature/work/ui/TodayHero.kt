package com.example.lifet.feature.work.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.work.WorkTodaySummary
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.work.DurationFormat
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.WorkFormatters
import com.example.lifet.feature.work.rememberDurationFormat
import com.example.lifet.feature.work.rememberLiveNow
import com.example.lifet.feature.work.rememberWorkFormatters
import java.time.ZoneId

private val BIG_BUTTON_HEIGHT = 64.dp
private const val HERO_MAX_SP = 52
private const val HERO_MIN_SP = 24

/**
 * Héroe de Hoy: la jornada en curso con su cronómetro en vivo y los tres botones grandes Entrada / Pausa / Salida.
 * El cronómetro es una hoja ([LiveStopwatch]) que lee el reloj por sí misma: cada segundo se recompone sólo su
 * texto, no la tarjeta ni la pantalla.
 */
@Composable
internal fun TodayHero(
    summary: WorkTodaySummary,
    zone: ZoneId,
    onClockIn: () -> Unit,
    onBreak: () -> Unit,
    onResume: () -> Unit,
    onClockOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val durations = rememberDurationFormat()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            HeroHeader(summary, formatters, zone)
            LiveStopwatch(summary, formatters)
            HeroTotals(summary, durations)
            ClockButtons(summary, onClockIn, onBreak, onResume, onClockOut)
        }
    }
}

@Composable
private fun HeroHeader(
    summary: WorkTodaySummary,
    formatters: WorkFormatters,
    zone: ZoneId,
) {
    val title =
        when {
            summary.isClockedIn && summary.onBreak -> R.string.work_hero_title_break
            summary.isClockedIn -> R.string.work_hero_title_in
            else -> R.string.work_hero_title_idle
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        Icon(Area.WORK.icon, contentDescription = null, tint = LifeTrackerTheme.areaColors.work)
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val start = summary.shiftStart
            Text(
                text =
                    if (start != null) {
                        stringResource(R.string.work_hero_since, formatters.clock(start, zone))
                    } else {
                        stringResource(R.string.work_hero_idle_hint)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Cronómetro. Con la jornada en marcha lee el "ahora" vivo; con pausa o sin jornada se queda fijo (no hay ningún
 * temporizador activo). Es la única parte de la pantalla que cambia cada segundo.
 */
@Composable
private fun LiveStopwatch(
    summary: WorkTodaySummary,
    formatters: WorkFormatters,
) {
    val ticking = summary.isClockedIn && !summary.onBreak
    val now by rememberLiveNow(enabled = ticking)
    val seconds = if (summary.isClockedIn) summary.shiftSecondsAt(now) else summary.todaySecondsAt(now)
    val text = formatters.stopwatch(seconds)
    val description = stringResource(R.string.work_hero_description, text)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val color = if (summary.isClockedIn) onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        // Con fuente al 200 % la cifra se encoge hasta HERO_MIN_SP en lugar de cortarse.
        autoSize = TextAutoSize.StepBased(minFontSize = HERO_MIN_SP.sp, maxFontSize = HERO_MAX_SP.sp),
    )
}

@Composable
private fun HeroTotals(
    summary: WorkTodaySummary,
    durations: DurationFormat,
) {
    // Sólo se vuelve a leer el reloj si hay una jornada en marcha; si no, las cifras no cambian solas.
    val now by rememberLiveNow(enabled = summary.isClockedIn && !summary.onBreak)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.bodyMedium
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.work_hero_today_total, durations.format(summary.todaySecondsAt(now))),
            style = style,
            color = muted,
        )
        Text(
            text =
                stringResource(
                    R.string.work_hero_week_total,
                    durations.format(summary.weekSecondsAt(now)),
                    durations.format(summary.weeklyTargetSeconds),
                ),
            style = style,
            color = muted,
        )
        Text(
            text = stringResource(R.string.work_hero_balance, durations.signed(summary.weekBalanceSecondsAt(now))),
            style = style,
            color = muted,
        )
    }
}

/** Entrada / Pausa (o Reanudar) / Salida. Sin jornada sólo se ofrece la entrada. */
@Composable
private fun ClockButtons(
    summary: WorkTodaySummary,
    onClockIn: () -> Unit,
    onBreak: () -> Unit,
    onResume: () -> Unit,
    onClockOut: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        if (!summary.isClockedIn) {
            val label = stringResource(R.string.work_clock_in)
            BigButton(label, Icons.AutoMirrored.Rounded.Login, onClockIn, primary = true)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
                if (summary.onBreak) {
                    BigButton(
                        label = stringResource(R.string.work_clock_resume),
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onResume,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    BigButton(
                        label = stringResource(R.string.work_clock_break),
                        icon = Icons.Rounded.Coffee,
                        onClick = onBreak,
                        modifier = Modifier.weight(1f),
                    )
                }
                BigButton(
                    label = stringResource(R.string.work_clock_out),
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    onClick = onClockOut,
                    outlined = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BigButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    outlined: Boolean = false,
) {
    val base = modifier.fillMaxWidth().heightIn(min = maxOf(BIG_BUTTON_HEIGHT, MinTouchTarget))
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    when {
        primary -> Button(onClick = onClick, modifier = base) { content() }
        outlined -> OutlinedButton(onClick = onClick, modifier = base) { content() }
        else -> FilledTonalButton(onClick = onClick, modifier = base) { content() }
    }
}
