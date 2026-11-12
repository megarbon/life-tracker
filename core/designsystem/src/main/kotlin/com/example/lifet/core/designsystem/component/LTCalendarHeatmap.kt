package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import java.time.LocalDate

private const val DAYS_IN_WEEK = 7
private const val MIN_ALPHA_MIX = 0.18f
private const val LEGEND_STEPS = 4
private const val PREVIEW_MODULO = 5
private const val PREVIEW_LEVEL_STEP = 0.25f

/**
 * Calendario de intensidad (estilo "contribuciones") para hábitos, deporte o ánimo.
 *
 * [intensity] asocia cada día con un valor de 0 a 1 (ausente = sin registro). Las filas son
 * semanas que empiezan en lunes. Es informativo: [summary] es lo que lee TalkBack.
 */
@Composable
public fun LTCalendarHeatmap(
    range: DateRange,
    intensity: Map<LocalDate, Float>,
    summary: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val first = range.start
    val leading = first.dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(leading) { null } + range.days
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
                repeat(DAYS_IN_WEEK) { index ->
                    val date = week.getOrNull(index)
                    HeatCell(
                        fill = date?.let { cellColor(intensity[it], empty, color) } ?: Color.Transparent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        HeatLegend(color = color, empty = empty)
    }
}

private fun cellColor(
    level: Float?,
    empty: Color,
    color: Color,
): Color =
    if (level == null || level <= 0f) {
        empty
    } else {
        lerp(empty, color, MIN_ALPHA_MIX + (1f - MIN_ALPHA_MIX) * level.coerceIn(0f, 1f))
    }

@Composable
private fun HeatCell(
    fill: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(fill))
}

@Composable
private fun HeatLegend(
    color: Color,
    empty: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.lt_heatmap_less), style = MaterialTheme.typography.labelSmall)
        repeat(LEGEND_STEPS + 1) { step ->
            val level = step.toFloat() / LEGEND_STEPS
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(cellColor(level, empty, color))
                    .semantics { },
            )
        }
        Text(stringResource(R.string.lt_heatmap_more), style = MaterialTheme.typography.labelSmall)
    }
}

@LTPreviews
@Composable
private fun LTCalendarHeatmapPreview() {
    LifeTrackerTheme {
        val range = DateRange.containing(DateRangeUnit.MONTH, LocalDate.of(2026, 3, 10))
        val data =
            range.days
                .withIndex()
                .filter { it.index % PREVIEW_MODULO != 0 }
                .associate { it.value to ((it.index % LEGEND_STEPS) + 1) * PREVIEW_LEVEL_STEP }
        LTCalendarHeatmap(
            range = range,
            intensity = data,
            summary = "Marzo: actividad en 25 de 31 días",
            color = LifeTrackerTheme.areaColors.fitness,
        )
    }
}
