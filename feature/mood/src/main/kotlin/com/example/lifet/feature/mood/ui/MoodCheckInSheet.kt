package com.example.lifet.feature.mood.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.lifet.core.designsystem.component.LTMultiChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodTags
import com.example.lifet.feature.mood.MoodFormState
import com.example.lifet.feature.mood.R
import com.example.lifet.feature.mood.emotionLabel
import com.example.lifet.feature.mood.factorLabel

/**
 * Hoja de check-in: el nivel arriba (lo único obligatorio) y el detalle desplegable debajo.
 * Al editar, el detalle se abre si la entrada ya lo tenía.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoodCheckInSheet(
    form: MoodFormState,
    onChange: ((MoodFormState) -> MoodFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    LTQuickAddSheet(
        title = stringResource(if (form.isEditing) R.string.mood_form_title_edit else R.string.mood_form_title_new),
        onDismiss = onDismiss,
    ) {
        MoodCheckInContent(form = form, onChange = onChange, onSave = onSave)
    }
}

@Composable
internal fun MoodCheckInContent(
    form: MoodFormState,
    onChange: ((MoodFormState) -> MoodFormState) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(form.hasDetail) }
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md)) {
        MoodLevelPicker(selected = form.level, onSelect = { level -> onChange { it.copy(level = level) } })
        if (form.level == null) {
            Text(
                text = stringResource(R.string.mood_form_level_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) {
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            Text(stringResource(if (expanded) R.string.mood_form_detail_hide else R.string.mood_form_detail_show))
        }
        AnimatedVisibility(visible = expanded) {
            MoodDetailFields(form = form, onChange = onChange)
        }
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onSave()
            },
            enabled = form.canSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.mood_form_save)) }
    }
}

@Composable
private fun MoodDetailFields(
    form: MoodFormState,
    onChange: ((MoodFormState) -> MoodFormState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md)) {
        TagSection(
            title = stringResource(R.string.mood_form_emotions),
            options = form.emotionOptions,
            selected = form.emotions.toSet(),
            label = { emotionLabel(it) },
            onToggle = { key -> onChange { it.toggleEmotion(key) } },
            input = form.emotionInput,
            inputLabel = stringResource(R.string.mood_form_custom_emotion),
            onInputChange = { text -> onChange { it.copy(emotionInput = text) } },
            onAdd = { onChange { it.addEmotionFromInput() } },
        )
        ScalePicker(
            label = stringResource(R.string.mood_form_energy),
            value = form.energy,
            onSelect = { value -> onChange { it.withEnergy(value) } },
        )
        ScalePicker(
            label = stringResource(R.string.mood_form_stress),
            value = form.stress,
            onSelect = { value -> onChange { it.withStress(value) } },
        )
        TagSection(
            title = stringResource(R.string.mood_form_factors),
            options = form.factorOptions,
            selected = form.factors.toSet(),
            label = { factorLabel(it) },
            onToggle = { key -> onChange { it.toggleFactor(key) } },
            input = form.factorInput,
            inputLabel = stringResource(R.string.mood_form_custom_factor),
            onInputChange = { text -> onChange { it.copy(factorInput = text) } },
            onAdd = { onChange { it.addFactorFromInput() } },
        )
        FreeText(
            value = form.note,
            label = stringResource(R.string.mood_form_note),
            onValueChange = { text -> onChange { it.copy(note = text) } },
        )
        FreeText(
            value = form.gratitude,
            label = stringResource(R.string.mood_form_gratitude),
            onValueChange = { text -> onChange { it.copy(gratitude = text) } },
        )
    }
}

@Composable
private fun TagSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    label: @Composable (String) -> String,
    onToggle: (String) -> Unit,
    input: String,
    inputLabel: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        val labels = options.associateWith { label(it) }
        LTMultiChipGroup(
            options = options,
            selected = selected,
            label = { labels.getValue(it) },
            onToggle = onToggle,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { onInputChange(it.take(MoodTags.MAX_LENGTH)) },
                label = { Text(inputLabel) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAdd, enabled = input.isNotBlank()) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.mood_form_add_tag))
            }
        }
    }
}

@Composable
private fun FreeText(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(MoodRules.TEXT_MAX_LENGTH)) },
        label = { Text(label) },
        minLines = 2,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
}

@LTPreviews
@Composable
private fun MoodCheckInContentPreview() {
    LifeTrackerTheme {
        Surface {
            MoodCheckInContent(
                form =
                    MoodFormState(
                        level = MoodLevel.GOOD,
                        emotions = listOf("joy", "calm"),
                        energy = 4,
                        factors = listOf("sport"),
                        note = "Paseo largo al atardecer",
                    ),
                onChange = {},
                onSave = {},
            )
        }
    }
}
