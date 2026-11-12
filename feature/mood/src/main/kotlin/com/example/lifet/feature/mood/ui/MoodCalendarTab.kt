package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.lifet.core.designsystem.component.LTCalendarHeatmap
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.feature.mood.MoodCalendar
import com.example.lifet.feature.mood.MoodFormatters
import com.example.lifet.feature.mood.R
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle

private const val MONTHS_PER_ROW = 2

/**
 * "Year in pixels": un cuadro por día del año con la intensidad del ánimo medio. El
 * [LTCalendarHeatmap] dibuja semanas de lunes a domingo; aquí se reparte por meses, de dos en dos,
 * para que cada bloque sea legible y el año quepa sin scroll horizontal.
 */
@Composable
internal fun MoodCalendarTab(
    calendar: MoodCalendar,
    formatters: MoodFormatters,
    onYearChange: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val year = calendar.range.start.year
    val average = calendar.average
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "header") {
            YearHeader(calendar, year, onYearChange)
        }
        item(key = "stats") {
            Text(
                text =
                    if (average == null) {
                        stringResource(R.string.mood_calendar_no_data)
                    } else {
                        stringResource(R.string.mood_calendar_stats, calendar.loggedDays, formatters.average(average))
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(Month.entries.chunked(MONTHS_PER_ROW), key = { it.first().value }) { months ->
            Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md)) {
                months.forEach { month ->
                    MonthBlock(month, calendar, formatters, Modifier.weight(1f))
                }
            }
        }
        item(key = "hint") {
            Text(
                text = stringResource(R.string.mood_calendar_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun YearHeader(
    calendar: MoodCalendar,
    year: Int,
    onYearChange: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onYearChange(year - 1) }, enabled = calendar.hasPreviousYear) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.mood_calendar_previous),
            )
        }
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onYearChange(year + 1) }, enabled = calendar.hasNextYear) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.mood_calendar_next),
            )
        }
    }
}

@Composable
private fun MonthBlock(
    month: Month,
    calendar: MoodCalendar,
    formatters: MoodFormatters,
    modifier: Modifier = Modifier,
) {
    val year = calendar.range.start.year
    val range = DateRange.containing(DateRangeUnit.MONTH, LocalDate.of(year, month, 1))
    val monthDays = calendar.intensity.filterKeys { it in range }
    val name = month.getDisplayName(TextStyle.FULL_STANDALONE, formatters.locale)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(
            text = name.replaceFirstChar { it.titlecase(formatters.locale) },
            style = MaterialTheme.typography.titleSmall,
        )
        LTCalendarHeatmap(
            range = range,
            intensity = monthDays,
            summary = stringResource(R.string.mood_calendar_month_summary, name, year, monthDays.size),
            color = LifeTrackerTheme.areaColors.mood,
        )
    }
}
