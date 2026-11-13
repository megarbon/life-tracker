package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.work.WorkTodaySummary
import com.example.lifet.core.ui.icon
import java.time.Clock
import java.time.Instant
import java.util.Locale
import kotlin.math.abs

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val PREVIEW_TODAY_SECONDS = 19_800L
private const val PREVIEW_WEEK_SECONDS = 72_000L
private const val PREVIEW_TARGET_SECONDS = 144_000L
private const val PREVIEW_BALANCE_SECONDS = 1_800L

/** Tarjeta de Trabajo en Hoy: horas de hoy y saldo de la semana; avisa con calma si una jornada quedó abierta. */
@Composable
internal fun WorkTodayCard(
    summary: WorkTodaySummary,
    clock: Clock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Solo con la jornada en marcha se lee el reloj vivo; sin ella las cifras del resumen ya son definitivas.
    val now = if (summary.liveToday || summary.liveWeek) rememberMinuteNow(clock).value else summary.asOf
    val todaySeconds = summary.todaySecondsAt(now)
    val weekSeconds = summary.weekSecondsAt(now)
    val weekBalance = summary.weekBalanceSecondsAt(now)
    val value =
        if (summary.hasData || summary.isClockedIn) {
            stringResource(R.string.dashboard_work_hours, formatHoursMinutes(todaySeconds))
        } else {
            stringResource(R.string.dashboard_work_empty_value)
        }
    val subtitle =
        when {
            summary.needsClosing -> {
                stringResource(R.string.dashboard_work_needs_closing)
            }

            summary.isClockedIn && summary.onBreak -> {
                stringResource(R.string.dashboard_work_on_break)
            }

            summary.isClockedIn -> {
                stringResource(R.string.dashboard_work_in_progress)
            }

            !summary.hasData -> {
                stringResource(R.string.dashboard_work_empty)
            }

            weekBalance >= 0 -> {
                stringResource(R.string.dashboard_work_balance_ahead, formatHoursMinutes(weekBalance))
            }

            else -> {
                stringResource(
                    R.string.dashboard_work_balance_behind,
                    formatHoursMinutes(abs(weekBalance)),
                )
            }
        }
    val progress =
        if (summary.weeklyTargetSeconds > 0) {
            (weekSeconds.toFloat() / summary.weeklyTargetSeconds).coerceIn(0f, 1f)
        } else {
            null
        }
    LTMetricCard(
        icon = Area.WORK.icon,
        title = stringResource(R.string.dashboard_work_title),
        value = value,
        subtitle = subtitle,
        progress = progress,
        accent = LifeTrackerTheme.areaColors.work,
        onClick = onClick,
        modifier = modifier,
    )
}

/** "7:05" con dígitos latinos fijos, para que no cambie con el idioma del móvil. */
private fun formatHoursMinutes(seconds: Long): String {
    val totalMinutes = seconds / SECONDS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return String.format(Locale.ROOT, "%d:%02d", hours, minutes)
}

internal fun previewWork(): WorkTodaySummary =
    WorkTodaySummary(
        asOf = Instant.parse("2026-03-10T12:00:00Z"),
        isClockedIn = false,
        shiftStart = null,
        onBreak = false,
        needsClosing = false,
        activeShiftSeconds = 0,
        todaySeconds = PREVIEW_TODAY_SECONDS,
        weekSeconds = PREVIEW_WEEK_SECONDS,
        weeklyTargetSeconds = PREVIEW_TARGET_SECONDS,
        weekBalanceSeconds = PREVIEW_BALANCE_SECONDS,
        totalBalanceSeconds = PREVIEW_BALANCE_SECONDS,
        focusSecondsToday = 0,
        hasData = true,
        liveToday = false,
        liveDaySurplusSeconds = 0,
        liveWeek = false,
    )

@LTPreviews
@Composable
private fun WorkTodayCardPreview() {
    LifeTrackerTheme { WorkTodayCard(summary = previewWork(), clock = Clock.systemUTC(), onClick = {}) }
}
