package com.example.lifet.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.component.LTProgressRing
import com.example.lifet.core.designsystem.component.RingSpec
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.NutritionTodaySummary
import com.example.lifet.core.model.nutrition.WaterEntry
import com.example.lifet.core.ui.icon
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PERCENT = 100
private const val RING_SIZE_DP = 112
private const val RING_STROKE_DP = 12
private const val PREVIEW_KCAL = 1_540.0
private const val PREVIEW_GOAL_KCAL = 2_000
private const val PREVIEW_PROTEIN = 90.0
private const val PREVIEW_CARBS = 180.0
private const val PREVIEW_FAT = 55.0
private const val PREVIEW_WATER_GOAL = 2_000
private const val PREVIEW_WATER = 750
private const val PREVIEW_MEALS = 3
private const val PREVIEW_GOAL_PROTEIN = 120
private const val PREVIEW_GOAL_CARBS = 230
private const val PREVIEW_GOAL_FAT = 65

/** Tarjeta de Alimentación en Hoy: anillo de kcal, agua y un toque para añadir 250 ml. */
@Composable
internal fun NutritionTodayCard(
    summary: NutritionTodaySummary,
    onOpen: () -> Unit,
    onAddWater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LifeTrackerTheme.areaColors.nutrition
    val format = NumberFormat.getIntegerInstance()
    val kcal = format.format(summary.consumed.kcal.roundToInt())
    val remaining = summary.remainingKcal
    val status =
        if (remaining >= 0) {
            stringResource(R.string.dashboard_nutrition_remaining, format.format(remaining))
        } else {
            stringResource(R.string.dashboard_nutrition_over, format.format(abs(remaining)))
        }
    val percent = (summary.kcalProgress * PERCENT).roundToInt()
    val progressText = stringResource(R.string.dashboard_nutrition_progress, percent)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
                modifier =
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "$kcal. $status. $progressText"
                    },
            ) {
                LTProgressRing(
                    rings = listOf(RingSpec(summary.kcalProgress.toFloat(), accent)),
                    size = RING_SIZE_DP.dp,
                    strokeWidth = RING_STROKE_DP.dp,
                    contentDescription = progressText,
                ) {
                    Icon(Area.NUTRITION.icon, contentDescription = null, tint = accent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_nutrition_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_nutrition_kcal, kcal),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_nutrition_goal, format.format(summary.goals.kcal)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = status, style = MaterialTheme.typography.bodyMedium)
                }
            }
            WaterRow(waterMl = summary.waterMl, onAddWater = onAddWater)
            TextButton(onClick = onOpen) { Text(stringResource(R.string.dashboard_nutrition_open)) }
        }
    }
}

@Composable
private fun WaterRow(
    waterMl: Int,
    onAddWater: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val water = NumberFormat.getIntegerInstance().format(waterMl)
        Text(
            text = stringResource(R.string.dashboard_nutrition_water, water),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        val addWaterLabel = stringResource(R.string.dashboard_nutrition_add_water, WaterEntry.GLASS_ML)
        FilledTonalButton(
            onClick = onAddWater,
            modifier = Modifier.heightIn(min = MinTouchTarget).semantics { contentDescription = addWaterLabel },
        ) {
            Icon(Icons.Rounded.WaterDrop, contentDescription = null)
            Text(stringResource(R.string.dashboard_nutrition_water_amount, WaterEntry.GLASS_ML))
        }
    }
}

internal fun previewNutrition(): NutritionTodaySummary =
    NutritionTodaySummary(
        date = LocalDate.parse("2026-03-10"),
        consumed = Nutrients(PREVIEW_KCAL, PREVIEW_PROTEIN, PREVIEW_CARBS, PREVIEW_FAT),
        goals =
            NutritionGoals(
                kcal = PREVIEW_GOAL_KCAL,
                proteinG = PREVIEW_GOAL_PROTEIN,
                carbsG = PREVIEW_GOAL_CARBS,
                fatG = PREVIEW_GOAL_FAT,
                waterMl = PREVIEW_WATER_GOAL,
            ),
        waterMl = PREVIEW_WATER,
        mealCount = PREVIEW_MEALS,
    )

@LTPreviews
@Composable
private fun NutritionTodayCardPreview() {
    LifeTrackerTheme {
        NutritionTodayCard(summary = previewNutrition(), onOpen = {}, onAddWater = {})
    }
}
