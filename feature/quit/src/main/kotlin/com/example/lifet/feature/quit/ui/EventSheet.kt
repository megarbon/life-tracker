package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.model.quit.QuitOverview
import com.example.lifet.core.model.quit.QuitRules
import com.example.lifet.feature.quit.ConsumptionDraft
import com.example.lifet.feature.quit.CravingDraft
import com.example.lifet.feature.quit.DraftIssue
import com.example.lifet.feature.quit.QuitEditor
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.blankConsumptionDraft
import com.example.lifet.feature.quit.blankCravingDraft
import com.example.lifet.feature.quit.intensityLabelRes
import com.example.lifet.feature.quit.issues
import com.example.lifet.feature.quit.labelRes
import com.example.lifet.feature.quit.toDraft
import com.example.lifet.feature.quit.validate
import java.time.Instant
import java.time.ZoneId

/** Una combinación de disparador y estrategia que el usuario ya ha usado: un toque rellena ambas. */
private data class RecentCombo(
    val trigger: CravingTrigger,
    val strategy: CopingStrategy?,
)

/**
 * Hoja de alta y edición de un antojo o de un consumo. Arriba, las combinaciones recientes de disparador y estrategia
 * como chips: un toque las aplica y registrar un antojo lleva unos pocos segundos. El tono es siempre el de apoyo:
 * un consumo se anota "sin culpa" y dice claramente que no borra nada.
 */
@Composable
internal fun EventSheet(
    overview: QuitOverview,
    editor: QuitEditor.Event,
    habit: QuitHabit,
    onDismiss: () -> Unit,
    onSave: (QuitSaved) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val zone = editor.existing?.zone ?: editor.zone
    val isEdit = editor.existing != null
    var asConsumption by rememberSaveable(editor.newId) { mutableStateOf(editor.asConsumption) }
    var cravingDraft by rememberSaveable(editor.newId) { mutableStateOf(initialCraving(editor)) }
    var consumptionDraft by rememberSaveable(editor.newId) { mutableStateOf(initialConsumption(editor)) }
    var submitted by rememberSaveable(editor.newId) { mutableStateOf(false) }
    val now = editor.now
    val combos = if (!asConsumption && !isEdit) recentCombos(overview, habit.id) else emptyList()
    val comboLabels = comboLabels(combos)

    LTQuickAddSheet(
        title = stringResource(sheetTitle(isEdit, asConsumption)),
        onDismiss = onDismiss,
        recents = comboLabels.map { it.second },
        onRecentPick = { label ->
            comboLabels.firstOrNull { it.second == label }?.first?.let { combo ->
                cravingDraft = cravingDraft.copy(trigger = combo.trigger, strategy = combo.strategy)
            }
        },
        modifier = modifier,
    ) {
        SheetColumn {
            if (!isEdit) {
                val cravingLabel = stringResource(R.string.quit_event_kind_craving)
                val consumptionLabel = stringResource(R.string.quit_event_kind_consumption)
                LTChipGroup(
                    options = listOf(false, true),
                    selected = asConsumption,
                    label = { if (it) consumptionLabel else cravingLabel },
                    onSelect = {
                        asConsumption = it
                        submitted = false
                    },
                )
            }
            if (asConsumption) {
                ConsumptionFields(consumptionDraft, issues(submitted, consumptionDraft, zone, now, habit.startedAt)) {
                    consumptionDraft = it
                }
            } else {
                CravingFields(cravingDraft, issues(submitted, cravingDraft, zone, now, habit.startedAt)) {
                    cravingDraft = it
                }
            }
            SheetActions(
                onSave = {
                    submitted = true
                    save(asConsumption, cravingDraft, consumptionDraft, zone, now, habit.startedAt, onSave)
                },
                onDelete = onDelete,
            )
        }
    }
}

/** Lo que la hoja entrega al guardar: el formulario de un antojo o el de un consumo. */
internal sealed interface QuitSaved {
    data class Craving(
        val draft: CravingDraft,
    ) : QuitSaved

    data class Consumption(
        val draft: ConsumptionDraft,
    ) : QuitSaved
}

private fun save(
    asConsumption: Boolean,
    craving: CravingDraft,
    consumption: ConsumptionDraft,
    zone: ZoneId,
    now: Instant,
    start: Instant,
    onSave: (QuitSaved) -> Unit,
) {
    if (asConsumption) {
        if (consumption.validate(zone, now, start).issues().isEmpty()) onSave(QuitSaved.Consumption(consumption))
    } else if (craving.validate(zone, now, start).issues().isEmpty()) {
        onSave(QuitSaved.Craving(craving))
    }
}

private fun issues(
    submitted: Boolean,
    draft: CravingDraft,
    zone: ZoneId,
    now: Instant,
    start: Instant,
): Set<DraftIssue> = if (submitted) draft.validate(zone, now, start).issues() else emptySet()

private fun issues(
    submitted: Boolean,
    draft: ConsumptionDraft,
    zone: ZoneId,
    now: Instant,
    start: Instant,
): Set<DraftIssue> = if (submitted) draft.validate(zone, now, start).issues() else emptySet()

private fun sheetTitle(
    isEdit: Boolean,
    asConsumption: Boolean,
): Int =
    when {
        asConsumption && isEdit -> R.string.quit_event_sheet_consumption_edit
        asConsumption -> R.string.quit_event_sheet_consumption
        isEdit -> R.string.quit_event_sheet_craving_edit
        else -> R.string.quit_event_sheet_craving
    }

private fun initialCraving(editor: QuitEditor.Event): CravingDraft =
    (editor.existing as? QuitCraving)?.toDraft()
        ?: blankCravingDraft(
            id = editor.newId,
            habitId = editor.habitId,
            today = editor.today,
            nowMinute = editor.nowMinute,
            strategy = editor.seedStrategy,
        )

private fun initialConsumption(editor: QuitEditor.Event): ConsumptionDraft =
    (editor.existing as? QuitConsumption)?.toDraft()
        ?: blankConsumptionDraft(editor.newId, editor.habitId, editor.today, editor.nowMinute)

/** Las últimas combinaciones distintas de disparador y estrategia de antojos del hábito, de la más reciente. */
private fun recentCombos(
    overview: QuitOverview,
    habitId: String,
): List<RecentCombo> =
    overview.events
        .filterIsInstance<QuitCraving>()
        .filter { it.habitId == habitId }
        .map { RecentCombo(it.trigger, it.strategy) }
        .distinct()
        .take(QuitRules.RECENT_CRAVING_COMBOS)

@Composable
private fun comboLabels(combos: List<RecentCombo>): List<Pair<RecentCombo, String>> {
    val triggerNames = CravingTrigger.entries.associateWith { stringResource(it.labelRes) }
    val strategyNames = CopingStrategy.entries.associateWith { stringResource(it.labelRes) }
    val format = stringResource(R.string.quit_recent_combo)
    return combos
        .map { combo ->
            val trigger = triggerNames.getValue(combo.trigger)
            val label = combo.strategy?.let { format.format(trigger, strategyNames.getValue(it)) } ?: trigger
            combo to label
        }.distinctBy { it.second }
}
