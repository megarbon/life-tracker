package com.example.lifet.feature.nutrition.diary

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.lifet.core.designsystem.component.LTProgressRing
import com.example.lifet.core.designsystem.component.RingSpec
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.NutritionFormatter
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter

private const val TABULAR_NUMBERS = "tnum"

/** Héroe del Diario: anillo de kcal con la cifra grande ("1.540" / "de 2.000 kcal") y lo que queda. */
@Composable
internal fun KcalHero(
    consumed: Nutrients,
    goals: NutritionGoals,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val progress = if (goals.kcal > 0) (consumed.kcal / goals.kcal).toFloat() else 0f
    val remaining = goals.kcal - consumed.kcal
    val ringDescription =
        stringResource(
            R.string.nutrition_ring_description,
            format.kcal(consumed.kcal),
            format.kcal(goals.kcal.toDouble()),
        )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            LTProgressRing(
                rings = listOf(RingSpec(progress, LifeTrackerTheme.areaColors.nutrition)),
                contentDescription = ringDescription,
            ) {
                RingCenter(format, consumed.kcal, goals.kcal)
            }
            Text(
                text = remainingText(remaining, format),
                style = MaterialTheme.typography.titleMedium,
                color = if (remaining < 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RingCenter(
    format: NutritionFormatter,
    kcal: Double,
    goal: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        AnimatedContent(targetState = format.kcal(kcal), label = "kcal") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TABULAR_NUMBERS),
            )
        }
        Text(
            text = stringResource(R.string.nutrition_kcal_goal_of, format.kcal(goal.toDouble())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun remainingText(
    remaining: Double,
    format: NutritionFormatter,
): String =
    if (remaining >= 0) {
        stringResource(R.string.nutrition_remaining_kcal, format.kcal(remaining))
    } else {
        stringResource(R.string.nutrition_over_kcal, format.kcal(-remaining))
    }

/** Tres barras (proteínas, hidratos, grasas) con lo consumido frente al objetivo en gramos. */
@Composable
internal fun MacroBars(
    consumed: Nutrients,
    goals: NutritionGoals,
    modifier: Modifier = Modifier,
) {
    val colors = LifeTrackerTheme.areaColors
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            val protein = stringResource(R.string.nutrition_macro_protein)
            MacroBar(protein, consumed.proteinG, goals.proteinG, colors.fitness)
            MacroBar(stringResource(R.string.nutrition_macro_carbs), consumed.carbsG, goals.carbsG, colors.nutrition)
            MacroBar(stringResource(R.string.nutrition_macro_fat), consumed.fatG, goals.fatG, colors.work)
        }
    }
}

@Composable
private fun MacroBar(
    label: String,
    grams: Double,
    goal: Int,
    color: Color,
) {
    val format = rememberNutritionFormatter()
    val goalText = format.amount(goal)
    val gramsText = format.grams(grams)
    val description = stringResource(R.string.nutrition_macro_description, label, gramsText, goalText)
    val progress = if (goal > 0) (grams / goal).toFloat().coerceIn(0f, 1f) else 0f
    Column(
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = stringResource(R.string.nutrition_macro_progress, gramsText, goalText),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_NUMBERS),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
