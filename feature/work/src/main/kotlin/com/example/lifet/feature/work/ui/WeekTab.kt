package com.example.lifet.feature.work.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.work.WorkWeekDay
import com.example.lifet.core.model.work.WorkWeekReport
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.work.DurationFormat
import com.example.lifet.feature.work.LocalWorkClock
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.WorkAction
import com.example.lifet.feature.work.WorkFormatters
import com.example.lifet.feature.work.rememberDurationFormat
import com.example.lifet.feature.work.rememberWorkFormatters

/**
 * Semana: navegación entre semanas, el total frente al objetivo, las horas de cada día (tocar un día añade o edita
 * jornada), las gráficas de horas por día y de saldo acumulado y el reparto por proyecto.
 */
@Composable
internal fun WeekTab(
    week: WorkWeekReport,
    contentPadding: PaddingValues,
    onAction: (WorkAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val durations = rememberDurationFormat()
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "nav", contentType = "nav") { WeekNavigator(week, formatters, onAction) }
        item(key = "hero", contentType = "hero") { WeekHero(week, durations) }
        item(key = "days-header", contentType = "header") {
            LTSectionHeader(title = stringResource(R.string.work_chart_hours_title))
        }
        items(week.days, key = { "day-${it.date}" }, contentType = { "day" }) { day ->
            WeekDayRow(
                day,
                formatters,
                durations,
                onClick = { onAction(WorkAction.OpenShift(day.shifts.firstOrNull(), day.date)) },
            )
        }
        item(key = "chart-hours", contentType = "chart") { HoursChart(week) }
        item(key = "chart-balance", contentType = "chart") { BalanceChart(week) }
        item(key = "chart-projects", contentType = "chart") {
            ProjectSharesChart(week.projectShares, weekly = true)
        }
    }
}

@Composable
private fun WeekNavigator(
    week: WorkWeekReport,
    formatters: WorkFormatters,
    onAction: (WorkAction) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onAction(WorkAction.MoveWeek(-1)) }) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.work_week_previous),
            )
        }
        Text(
            text = formatters.weekTitle(week.week),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = { onAction(WorkAction.MoveWeek(1)) }, enabled = !week.isCurrentWeek) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.work_week_next),
            )
        }
    }
    if (!week.isCurrentWeek) {
        TextButton(onClick = { onAction(WorkAction.MoveWeek(0)) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.work_week_current))
        }
    }
}

@Composable
private fun WeekHero(
    week: WorkWeekReport,
    durations: DurationFormat,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
            ) {
                Icon(Area.WORK.icon, contentDescription = null, tint = LifeTrackerTheme.areaColors.work)
                Text(
                    text = stringResource(R.string.work_week_hero_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = durations.format(week.workedSeconds),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.work_week_hero_target, durations.format(week.weeklyTargetSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.work_week_hero_balance, durations.signed(week.balanceSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Un día: su nombre, las horas trabajadas y el objetivo (o "Día libre" si no se trabaja ese día). */
@Composable
private fun WeekDayRow(
    day: WorkWeekDay,
    formatters: WorkFormatters,
    durations: DurationFormat,
    onClick: () -> Unit,
) {
    val title = formatters.dayTitle(day.date)
    val clockNow = LocalWorkClock.current.instant()
    val worked =
        when {
            day.workedSeconds > 0L -> durations.format(day.workedSeconds)
            day.shifts.any { it.isOpen && !it.isForgotten(clockNow) } -> stringResource(R.string.work_shift_open)
            day.shifts.any { it.isOpen } -> stringResource(R.string.work_shift_open_forgotten)
            day.shifts.isNotEmpty() -> durations.format(0L)
            else -> stringResource(R.string.work_week_day_empty)
        }
    val target =
        if (day.targetSeconds > 0L) {
            stringResource(R.string.work_week_day_target, durations.format(day.targetSeconds))
        } else {
            stringResource(R.string.work_week_day_free)
        }
    val description = stringResource(R.string.work_week_day_row, title, worked, target)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(enabled = !day.isFuture, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = target,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(worked, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}
