package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Selector Día/Semana/Mes/Año con flechas para moverse en el tiempo.
 * [title] es el texto del rango actual (usa [defaultRangeTitle] si no necesitas uno propio).
 * La flecha "siguiente" se desactiva cuando el rango ya contiene [today] (no hay futuro que mostrar).
 */
@Composable
public fun LTDateRangeSelector(
    range: DateRange,
    today: LocalDate,
    onRangeChange: (DateRange) -> Unit,
    modifier: Modifier = Modifier,
    title: String = defaultRangeTitle(range),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        val units = DateRangeUnit.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            units.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = unit == range.unit,
                    onClick = { onRangeChange(DateRange.containing(unit, today)) },
                    shape = SegmentedButtonDefaults.itemShape(index, units.size),
                ) { Text(stringResource(unit.labelRes())) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onRangeChange(range.shift(-1)) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.lt_previous),
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            // Sin futuro que mostrar: se bloquea al llegar al rango que contiene hoy.
            val hasNext = today !in range && range.endExclusive <= today
            IconButton(onClick = { onRangeChange(range.shift(1)) }, enabled = hasNext) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.lt_next),
                )
            }
        }
    }
}

private fun DateRangeUnit.labelRes(): Int =
    when (this) {
        DateRangeUnit.DAY -> R.string.lt_range_day
        DateRangeUnit.WEEK -> R.string.lt_range_week
        DateRangeUnit.MONTH -> R.string.lt_range_month
        DateRangeUnit.YEAR -> R.string.lt_range_year
    }

/** Título legible del rango según el idioma del dispositivo. */
@Composable
public fun defaultRangeTitle(
    range: DateRange,
    locale: Locale = Locale.getDefault(),
): String {
    val medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return when (range.unit) {
        DateRangeUnit.DAY -> range.start.format(medium)
        DateRangeUnit.WEEK -> "${range.start.format(medium)} – ${range.endInclusive.format(medium)}"
        DateRangeUnit.MONTH -> range.start.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        DateRangeUnit.YEAR -> range.start.year.toString()
    }
}

@LTPreviews
@Composable
private fun LTDateRangeSelectorPreview() {
    LifeTrackerTheme {
        val today = LocalDate.parse("2026-03-10")
        LTDateRangeSelector(
            range = DateRange.containing(DateRangeUnit.WEEK, today.minusWeeks(1)),
            today = today,
            onRangeChange = {},
        )
    }
}
