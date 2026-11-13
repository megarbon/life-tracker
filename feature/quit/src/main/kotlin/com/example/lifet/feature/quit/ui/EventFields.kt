package com.example.lifet.feature.quit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.QuitRules
import com.example.lifet.feature.quit.ConsumptionDraft
import com.example.lifet.feature.quit.CravingDraft
import com.example.lifet.feature.quit.DraftIssue
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.intensityLabelRes
import com.example.lifet.feature.quit.labelRes
import java.time.LocalDate

private const val NOTE_MAX_LINES = 3
private const val INTENSITY_STEPS = QuitRules.MAX_INTENSITY - QuitRules.MIN_INTENSITY - 1

@Composable
internal fun CravingFields(
    draft: CravingDraft,
    issues: Set<DraftIssue>,
    onChange: (CravingDraft) -> Unit,
) {
    Text(
        text = stringResource(R.string.quit_craving_reassurance),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    IntensityPicker(draft.intensity) { onChange(draft.copy(intensity = it)) }
    Text(stringResource(R.string.quit_field_trigger), style = MaterialTheme.typography.labelLarge)
    val triggers = CravingTrigger.entries.associateWith { stringResource(it.labelRes) }
    LTChipGroup(
        options = CravingTrigger.entries,
        selected = draft.trigger,
        label = { triggers.getValue(it) },
        onSelect = { onChange(draft.copy(trigger = it)) },
    )
    Text(stringResource(R.string.quit_field_strategy), style = MaterialTheme.typography.labelLarge)
    val none = stringResource(R.string.quit_field_strategy_none)
    val strategies = CopingStrategy.entries.associateWith { stringResource(it.labelRes) }
    LTChipGroup(
        options = listOf<CopingStrategy?>(null) + CopingStrategy.entries,
        selected = draft.strategy,
        label = { it?.let(strategies::getValue) ?: none },
        onSelect = { onChange(draft.copy(strategy = it)) },
    )
    MomentFields(
        draft.date,
        draft.minute,
        issues,
    ) { date, minute -> onChange(draft.copy(date = date, minute = minute)) }
    TextInput(
        value = draft.note,
        onValueChange = { onChange(draft.copy(note = it)) },
        label = stringResource(R.string.quit_field_note),
        maxLength = QuitRules.NOTE_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth(),
        maxLines = NOTE_MAX_LINES,
    )
}

@Composable
internal fun ConsumptionFields(
    draft: ConsumptionDraft,
    issues: Set<DraftIssue>,
    onChange: (ConsumptionDraft) -> Unit,
) {
    Text(
        text = stringResource(R.string.quit_consumption_reassurance),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NumberField(
        value = draft.unitsText,
        onValueChange = { onChange(draft.copy(unitsText = it)) },
        label = stringResource(R.string.quit_field_units),
        modifier = Modifier.fillMaxWidth(),
        error = issueText(issues, DraftIssue.UNITS_MISSING, DraftIssue.UNITS_INVALID),
    )
    Text(stringResource(R.string.quit_field_trigger_optional), style = MaterialTheme.typography.labelLarge)
    val none = stringResource(R.string.quit_field_strategy_none)
    val triggers = CravingTrigger.entries.associateWith { stringResource(it.labelRes) }
    LTChipGroup(
        options = listOf<CravingTrigger?>(null) + CravingTrigger.entries,
        selected = draft.trigger,
        label = { it?.let(triggers::getValue) ?: none },
        onSelect = { onChange(draft.copy(trigger = it)) },
    )
    MomentFields(
        draft.date,
        draft.minute,
        issues,
    ) { date, minute -> onChange(draft.copy(date = date, minute = minute)) }
    TextInput(
        value = draft.note,
        onValueChange = { onChange(draft.copy(note = it)) },
        label = stringResource(R.string.quit_field_note),
        maxLength = QuitRules.NOTE_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth(),
        maxLines = NOTE_MAX_LINES,
    )
}

/** Día y hora del registro; los errores de "futuro" y "antes del inicio" salen debajo. */
@Composable
private fun MomentFields(
    date: LocalDate,
    minute: Int,
    issues: Set<DraftIssue>,
    onChange: (LocalDate, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        DateField(
            date = date,
            onDateChange = { onChange(it, minute) },
            labelRes = R.string.quit_field_date,
            modifier = Modifier.fillMaxWidth(),
        )
        TimeField(
            minuteOfDay = minute,
            onChange = { onChange(date, it) },
            labelRes = R.string.quit_field_time,
            modifier = Modifier.fillMaxWidth(),
        )
        issueText(issues, DraftIssue.EVENT_IN_FUTURE, DraftIssue.EVENT_BEFORE_START)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Intensidad de 1 a 5 con una palabra que la acompaña ("media", "fuerte"): el valor nunca es sólo un número ni un
 * color. El control es accesible: TalkBack lo lee como "intensidad: fuerte".
 */
@Composable
private fun IntensityPicker(
    intensity: Int,
    onChange: (Int) -> Unit,
) {
    val label = stringResource(intensityLabelRes(intensity))
    val title = stringResource(R.string.quit_field_intensity, label)
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(QuitRules.MIN_INTENSITY, QuitRules.MAX_INTENSITY)) },
            valueRange = QuitRules.MIN_INTENSITY.toFloat()..QuitRules.MAX_INTENSITY.toFloat(),
            steps = INTENSITY_STEPS,
            colors =
                SliderDefaults.colors(
                    thumbColor = LifeTrackerTheme.areaColors.quit,
                    activeTrackColor = LifeTrackerTheme.areaColors.quit,
                ),
            modifier =
                Modifier.semantics {
                    contentDescription = title
                    stateDescription = label
                },
        )
    }
}
