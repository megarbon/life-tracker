package com.example.lifet.feature.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.labelRes
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter

/** Una tarjeta por comida (desayuno, comida, cena, snack) con sus alimentos y un botón para añadir más. */
@Composable
internal fun MealSectionCard(
    section: MealSection,
    onAdd: (MealType) -> Unit,
    onEdit: (MealEntry) -> Unit,
    onDelete: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val label = stringResource(section.type.labelRes)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = LifeTrackerTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = LifeTrackerTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.nutrition_kcal_value, format.kcal(section.kcal)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { onAdd(section.type) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.nutrition_add_to_meal, label))
                }
            }
            if (section.entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.nutrition_meal_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            horizontal = LifeTrackerTheme.spacing.md,
                            vertical = LifeTrackerTheme.spacing.sm,
                        ),
                )
            } else {
                section.entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = LifeTrackerTheme.spacing.md))
                    EntryRow(entry, onEdit = { onEdit(entry) }, onDelete = { onDelete(entry) })
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: MealEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val format = rememberNutritionFormatter()
    val kcal = format.kcal(entry.nutrients.kcal)
    val description =
        stringResource(R.string.nutrition_entry_description, entry.foodName, format.amount(entry.grams), kcal)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Zona editable: se lee como una sola fila y el botón de borrar queda como acción propia para TalkBack.
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget)
                    .clickable(onClick = onEdit)
                    .padding(start = LifeTrackerTheme.spacing.md)
                    .semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = LifeTrackerTheme.spacing.sm)) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.nutrition_grams_value, format.amount(entry.grams)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.nutrition_kcal_value, kcal),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.nutrition_delete))
        }
    }
}
