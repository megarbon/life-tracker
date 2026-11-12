package com.example.lifet.feature.nutrition.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter

/** Agua del día: progreso, botón "+250 ml" de un toque y otro para quitar el último vaso. */
@Composable
internal fun WaterCard(
    waterMl: Int,
    goalMl: Int,
    canRemove: Boolean,
    onAdd: () -> Unit,
    onRemoveLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val progressText = stringResource(R.string.nutrition_water_progress, format.amount(waterMl), format.amount(goalMl))
    val title = stringResource(R.string.nutrition_water)
    val progress = if (goalMl > 0) (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $progressText" },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = LifeTrackerTheme.areaColors.work,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(start = LifeTrackerTheme.spacing.sm),
                    )
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = LifeTrackerTheme.areaColors.work,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
            WaterActions(onAdd = onAdd, onRemoveLast = onRemoveLast, canRemove = canRemove)
        }
    }
}

@Composable
private fun WaterActions(
    onAdd: () -> Unit,
    onRemoveLast: () -> Unit,
    canRemove: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        val addDescription = stringResource(R.string.nutrition_add_water_description)
        Button(
            onClick = onAdd,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget)
                    .semantics { contentDescription = addDescription },
        ) { Text(stringResource(R.string.nutrition_add_water)) }
        IconButton(onClick = onRemoveLast, enabled = canRemove) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.nutrition_remove_water_description),
            )
        }
    }
}
