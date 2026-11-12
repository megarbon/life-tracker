package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.feature.mood.icon
import com.example.lifet.feature.mood.labelRes

/**
 * Cinco botones de ánimo, de peor a mejor. Cada uno lleva icono y texto, así el nivel nunca depende sólo del
 * color, y forman un grupo de selección única para TalkBack.
 */
@Composable
internal fun MoodLevelPicker(
    selected: MoodLevel?,
    onSelect: (MoodLevel) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = LifeTrackerTheme.areaColors.mood,
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        MoodLevel.entries.forEach { level ->
            val isSelected = level == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = MinTouchTarget)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(level) }),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = LifeTrackerTheme.spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
                ) {
                    Icon(
                        imageVector = level.icon,
                        contentDescription = null,
                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = stringResource(level.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@LTPreviews
@Composable
private fun MoodLevelPickerPreview() {
    LifeTrackerTheme {
        Surface { MoodLevelPicker(selected = MoodLevel.GOOD, onSelect = {}) }
    }
}
