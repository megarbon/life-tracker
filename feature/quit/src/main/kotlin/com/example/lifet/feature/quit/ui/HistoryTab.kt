package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitEvent
import com.example.lifet.core.model.quit.QuitOverview
import com.example.lifet.core.model.quit.QuitRules
import com.example.lifet.feature.quit.QuitFormatters
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.icon
import com.example.lifet.feature.quit.labelRes
import com.example.lifet.feature.quit.rememberQuitFormatters
import java.time.LocalDate
import java.time.LocalTime

private val ICON_BADGE = 40.dp
private val BADGE_ICON = 20.dp

/** Dentro de estos minutos de la medianoche se avisa de que el registro cae cerca del cambio de día. */
private const val MIDNIGHT_WINDOW_MINUTES = 30

/** Un día del historial con sus eventos, del más reciente al más antiguo. */
private data class HistoryDay(
    val date: LocalDate,
    val events: List<QuitEvent>,
)

/**
 * Historial de antojos y consumos de todos los hábitos, agrupado por día. Tocar una fila la edita; borrar muestra
 * "Deshacer". Un consumo no lleva rojo ni icono de alerta: es una marca en el camino, como un antojo.
 */
@Composable
internal fun HistoryTab(
    overview: QuitOverview,
    contentPadding: PaddingValues,
    onOpen: (QuitEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberQuitFormatters()
    val days = remember(overview.events) { groupByDay(overview.events) }
    val habitNames = overview.habits.associate { it.habit.id to it.habit.name }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        if (days.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.quit_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        days.forEach { day ->
            item(key = "day-${day.date}", contentType = "header") {
                LTSectionHeader(title = formatters.date(day.date))
            }
            items(day.events, key = { it.id }, contentType = { "event" }) { event ->
                EventRow(
                    event = event,
                    habitName = if (overview.habits.size > 1) habitNames[event.habitId] else null,
                    formatters = formatters,
                    onClick = { onOpen(event) },
                )
            }
        }
    }
}

private fun groupByDay(events: List<QuitEvent>): List<HistoryDay> =
    events
        .groupBy { it.date }
        .toSortedMap(compareByDescending { it })
        .map { (date, items) -> HistoryDay(date, items) }

@Composable
private fun EventRow(
    event: QuitEvent,
    habitName: String?,
    formatters: QuitFormatters,
    onClick: () -> Unit,
) {
    val accent = LifeTrackerTheme.areaColors.quit
    val title = eventTitle(event)
    val local = event.at.atZone(event.zone).toLocalTime()
    val time = local.format(formatters.time)
    val nearMidnight = local.isNearMidnight()
    val subtitle = eventSubtitle(event, habitName, time, nearMidnight)
    val description = listOfNotNull(title, subtitle, event.note.takeIf { it.isNotBlank() }).joinToString(". ")
    Row(
        modifier =
            Modifier
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
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(ICON_BADGE),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(event.icon, contentDescription = null, tint = accent, modifier = Modifier.size(BADGE_ICON))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.note.isNotBlank()) {
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = LifeTrackerTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun eventTitle(event: QuitEvent): String =
    when (event) {
        is QuitCraving -> {
            stringResource(R.string.quit_history_craving, event.intensity, QuitRules.MAX_INTENSITY)
        }

        is QuitConsumption -> {
            val units = pluralStringResource(R.plurals.quit_units, event.units, event.units)
            stringResource(R.string.quit_history_consumption, units)
        }
    }

/** Hora, lo que lo provocó y, si hay varios hábitos, a cuál pertenece. Los antojos añaden lo que se hizo. */
@Composable
private fun eventSubtitle(
    event: QuitEvent,
    habitName: String?,
    time: String,
    nearMidnight: Boolean,
): String {
    val trigger =
        when (event) {
            is QuitCraving -> stringResource(event.trigger.labelRes)
            is QuitConsumption -> event.trigger?.let { stringResource(it.labelRes) }
        }
    val strategy = (event as? QuitCraving)?.strategy?.let { stringResource(it.labelRes) }
    val midnight = stringResource(R.string.quit_history_midnight).takeIf { nearMidnight }
    return listOfNotNull(time, trigger, strategy, midnight, habitName?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
}

private fun LocalTime.isNearMidnight(): Boolean {
    val minutes = hour * MINUTES_IN_HOUR + minute
    return minutes < MIDNIGHT_WINDOW_MINUTES || minutes >= MINUTES_IN_DAY - MIDNIGHT_WINDOW_MINUTES
}

private const val MINUTES_IN_HOUR = 60
private const val MINUTES_IN_DAY = 1_440
