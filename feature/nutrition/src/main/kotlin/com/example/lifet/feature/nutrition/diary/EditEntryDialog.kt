package com.example.lifet.feature.nutrition.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.labelRes

/** Cambia la cantidad y la comida de un alimento ya registrado. */
@Composable
internal fun EditEntryDialog(
    entry: MealEntry,
    onDismiss: () -> Unit,
    onSave: (grams: Int, mealType: MealType) -> Unit,
) {
    var gramsText by rememberSaveable(entry.id) { mutableStateOf(entry.grams.toString()) }
    var mealName by rememberSaveable(entry.id) { mutableStateOf(entry.mealType.name) }
    val grams = gramsText.trim().toIntOrNull()?.takeIf { it in MealEntry.MIN_GRAMS..MealEntry.MAX_GRAMS }
    val mealType = MealType.fromName(mealName) ?: entry.mealType
    val labels = MealType.entries.associateWith { stringResource(it.labelRes) }
    val gramsError: @Composable () -> Unit = { Text(stringResource(R.string.nutrition_grams_error)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nutrition_edit_entry_title, entry.foodName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md)) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it.filter(Char::isDigit).take(MAX_DIGITS) },
                    label = { Text(stringResource(R.string.nutrition_grams_label)) },
                    singleLine = true,
                    isError = grams == null,
                    supportingText = if (grams == null) gramsError else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                LTChipGroup(
                    options = MealType.entries,
                    selected = mealType,
                    label = { labels.getValue(it) },
                    onSelect = { mealName = it.name },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { grams?.let { onSave(it, mealType) } }, enabled = grams != null) {
                Text(stringResource(R.string.nutrition_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.nutrition_cancel)) } },
    )
}

private const val MAX_DIGITS = 4
