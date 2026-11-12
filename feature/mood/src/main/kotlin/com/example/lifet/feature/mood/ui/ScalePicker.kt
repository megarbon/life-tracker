package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.feature.mood.R

/** Escala opcional de 1 a 5 (energía, estrés). Volver a tocar el valor elegido lo quita. */
@Composable
internal fun ScalePicker(
    label: String,
    value: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        val options = (MoodRules.SCALE_MIN..MoodRules.SCALE_MAX).toList()
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val description = stringResource(R.string.mood_form_scale_option, label, option)
                SegmentedButton(
                    selected = option == value,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.heightIn(min = MinTouchTarget).semantics { contentDescription = description },
                    icon = {},
                ) { Text(option.toString()) }
            }
        }
    }
}

@LTPreviews
@Composable
private fun ScalePickerPreview() {
    LifeTrackerTheme { Surface { ScalePicker(label = "Energía", value = 3, onSelect = {}) } }
}
