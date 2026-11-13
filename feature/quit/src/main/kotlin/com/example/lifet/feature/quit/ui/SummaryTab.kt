package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.component.LTStatTile
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.HabitProgress
import com.example.lifet.core.model.quit.QuitOverview
import com.example.lifet.core.model.quit.TriggerCount
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.labelRes
import com.example.lifet.feature.quit.rememberQuitFormatters
import java.time.Clock

/** Disparadores que enseña la lista del resumen. */
private const val TRIGGERS_SHOWN = 5

/**
 * Resumen de un hábito: el héroe (tiempo limpio en vivo y euros ahorrados), las tres acciones que importan (anotar un
 * antojo, pedir ayuda y anotar un consumo), los datos de apoyo, el próximo hito y las gráficas.
 */
@Composable
internal fun SummaryTab(
    overview: QuitOverview,
    progress: HabitProgress,
    selectedId: String,
    clock: Clock,
    contentPadding: PaddingValues,
    callbacks: SummaryCallbacks,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "picker") {
            HabitPicker(
                habits = overview.habits.map { it.habit },
                selectedId = selectedId,
                onSelect = callbacks.onSelectHabit,
                onAdd = callbacks.onAddHabit,
            )
        }
        item(key = "hero") { HeroCard(progress = progress, clock = clock) }
        item(key = "actions") { ActionButtons(progress.habit.id, callbacks) }
        item(key = "stats") { StatsRow(progress) }
        if (progress.habit.mode == HabitMode.QUIT) {
            item(key = "next") { NextMilestoneCard(progress, clock) }
        } else {
            item(key = "week-chart") { WeeklyUseChart(progress) }
        }
        item(key = "hours") { CravingHoursChart(progress) }
        item(key = "savings") { SavingsChart(progress) }
        item(key = "heatmap") { CleanDaysHeatmapCard(progress, overview.today) }
        if (progress.topTriggers.isNotEmpty()) {
            item(key = "triggers") { TriggersSection(progress.topTriggers) }
        }
        item(key = "edit") {
            TextButton(
                onClick = { callbacks.onEditHabit(progress) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Text(
                    text = stringResource(R.string.quit_edit_habit),
                    modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
                )
            }
        }
    }
}

/** Lo que el resumen puede pedir a la pantalla; un único objeto para no repartir una docena de lambdas. */
internal class SummaryCallbacks(
    val onSelectHabit: (String) -> Unit,
    val onAddHabit: () -> Unit,
    val onEditHabit: (HabitProgress) -> Unit,
    val onCraving: (String) -> Unit,
    val onConsumption: (String) -> Unit,
    val onSos: (String) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtons(
    habitId: String,
    callbacks: SummaryCallbacks,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        Button(
            onClick = { callbacks.onSos(habitId) },
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        ) {
            Icon(Icons.Rounded.Air, contentDescription = null)
            Text(
                text = stringResource(R.string.quit_action_sos),
                modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            FilledTonalButton(
                onClick = { callbacks.onCraving(habitId) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Waves, contentDescription = null)
                Text(
                    text = stringResource(R.string.quit_action_craving),
                    modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
                )
            }
            OutlinedButton(
                onClick = { callbacks.onConsumption(habitId) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Flag, contentDescription = null)
                Text(
                    text = stringResource(R.string.quit_action_consumption),
                    modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(progress: HabitProgress) {
    val formatters = rememberQuitFormatters()
    val none = stringResource(R.string.quit_stat_none)
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        if (progress.cravingsToday > 0) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.quit_craving_today,
                        progress.cravingsToday,
                        progress.cravingsToday,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            LTStatTile(
                label = stringResource(R.string.quit_stat_craving_today),
                value = formatters.number(progress.cravingsToday),
                supporting =
                    stringResource(
                        R.string.quit_stat_cravings_in_total,
                        formatters.number(progress.cravingsTotal),
                    ),
            )
            LTStatTile(
                label = stringResource(R.string.quit_stat_avoided),
                value = avoidedText(progress.avoidedUnits),
                supporting = stringResource(R.string.quit_stat_avoided_hint),
            )
            LTStatTile(
                label = stringResource(R.string.quit_stat_slips),
                value = if (progress.slips == 0) none else formatters.number(progress.slips),
                supporting = stringResource(R.string.quit_stat_slips_hint),
            )
        }
    }
}

/** Unidades evitadas con separador de miles y el plural correcto; el plural se decide con la cantidad entera. */
@Composable
private fun avoidedText(units: Long): String {
    val formatters = rememberQuitFormatters()
    val plural = units.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return pluralStringResource(R.plurals.quit_stat_avoided_units, plural, formatters.number(units))
}

@Composable
private fun TriggersSection(triggers: List<TriggerCount>) {
    val formatters = rememberQuitFormatters()
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        LTSectionHeader(title = stringResource(R.string.quit_triggers_title))
        triggers.take(TRIGGERS_SHOWN).forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(item.trigger.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(text = formatters.number(item.count), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
