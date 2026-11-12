@file:Suppress("MagicNumber") // Datos de ejemplo para previews.

package com.example.lifet.feature.mood

import androidx.compose.runtime.Composable
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodStats
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.ZoneId

// Datos de preview deterministas. No se usa core:demo (sólo existe en debug y una feature no debe depender de él).
private val PREVIEW_ZONE: ZoneId = ZoneId.of("Europe/Madrid")
private val PREVIEW_TODAY: LocalDate = LocalDate.of(2026, 3, 10)
private const val PREVIEW_DAYS = 60L
private const val WEEK = 7
private val PREVIEW_PATTERN = intArrayOf(2, 3, 3, 4, 4, 5, 4)
private val PREVIEW_EMOTIONS = listOf(listOf("joy"), listOf("tired", "anxiety"), listOf("calm", "gratitude"))
private val PREVIEW_FACTORS = listOf(listOf("sport", "sleep"), listOf("work"), listOf("social", "sport"))

private fun previewEntries(lowStreak: Boolean): List<MoodEntry> =
    (0L until PREVIEW_DAYS).map { daysAgo ->
        val date = PREVIEW_TODAY.minusDays(daysAgo)
        val value = if (lowStreak && daysAgo in 1L..5L) MoodLevel.LOW.value else PREVIEW_PATTERN[date.dayOfWeek.ordinal]
        val index = (daysAgo % PREVIEW_EMOTIONS.size).toInt()
        MoodEntry(
            id = daysAgo + 1,
            instant = date.atTime(9 + (daysAgo % WEEK).toInt(), 0).atZone(PREVIEW_ZONE).toInstant(),
            zone = PREVIEW_ZONE,
            level = MoodLevel.fromValue(value) ?: MoodLevel.NEUTRAL,
            emotions = PREVIEW_EMOTIONS[index],
            factors = PREVIEW_FACTORS[index],
            note = if (daysAgo == 0L) "Paseo largo al atardecer 🌅" else "",
        )
    }

private fun edgeEntries(): List<MoodEntry> =
    listOf(
        MoodEntry(
            id = 1,
            instant = PREVIEW_TODAY.atTime(0, 0, 1).atZone(PREVIEW_ZONE).toInstant(),
            zone = PREVIEW_ZONE,
            level = MoodLevel.VERY_LOW,
            emotions = listOf("una emoción con un nombre larguísimo que no cabe", "🥳"),
            note = "Texto larguísimo para comprobar que nada se corta ni rompe el diseño. ".repeat(8),
        ),
    )

internal fun previewSuccess(
    entries: List<MoodEntry>,
    supportVisible: Boolean = false,
): MoodUiState.Success {
    val range = DateRange.containing(DateRangeUnit.MONTH, PREVIEW_TODAY)
    val year = DateRange.containing(DateRangeUnit.YEAR, PREVIEW_TODAY)
    val daily = MoodStats.dailyAverages(entries)
    return MoodUiState.Success(
        today = PREVIEW_TODAY,
        summary = MoodStats.todaySummary(entries, PREVIEW_TODAY),
        series = MoodStats.series(range, entries),
        journal =
            entries
                .groupBy { it.localDate }
                .map { (date, day) -> MoodDayGroup(date, daily.getValue(date), day.toImmutableList()) }
                .sortedByDescending { it.date }
                .toImmutableList(),
        calendar =
            MoodCalendar(
                range = year,
                intensity = daily.mapValues { moodIntensity(it.value) },
                loggedDays = daily.size,
                average = daily.values.average(),
                hasPreviousYear = true,
                hasNextYear = false,
            ),
        supportVisible = supportVisible,
    )
}

@LTPreviews
@Composable
private fun MoodScreenEmptyPreview() {
    LifeTrackerTheme { MoodScreen(state = MoodUiState.Empty, overlay = MoodOverlay.None, onAction = {}) }
}

@LTPreviews
@Composable
private fun MoodScreenLoadingPreview() {
    LifeTrackerTheme { MoodScreen(state = MoodUiState.Loading, overlay = MoodOverlay.None, onAction = {}) }
}

@LTPreviews
@Composable
private fun MoodScreenErrorPreview() {
    LifeTrackerTheme {
        MoodScreen(state = MoodUiState.Error(R.string.mood_error_load), overlay = MoodOverlay.None, onAction = {})
    }
}

@LTPreviews
@Composable
private fun MoodScreenRegularPreview() {
    LifeTrackerTheme {
        MoodScreen(
            state = previewSuccess(previewEntries(lowStreak = true), supportVisible = true),
            overlay = MoodOverlay.None,
            onAction = {},
        )
    }
}

@LTPreviews
@Composable
private fun MoodScreenJournalPreview() {
    LifeTrackerTheme {
        MoodScreen(
            state = previewSuccess(previewEntries(lowStreak = false)),
            overlay = MoodOverlay.None,
            onAction = {},
            initialTab = MoodTab.JOURNAL,
        )
    }
}

@LTPreviews
@Composable
private fun MoodScreenCalendarPreview() {
    LifeTrackerTheme {
        MoodScreen(
            state = previewSuccess(previewEntries(lowStreak = false)),
            overlay = MoodOverlay.None,
            onAction = {},
            initialTab = MoodTab.CALENDAR,
        )
    }
}

@LTPreviews
@Composable
private fun MoodScreenEdgeCasesPreview() {
    LifeTrackerTheme {
        MoodScreen(
            state = previewSuccess(edgeEntries()),
            overlay = MoodOverlay.None,
            onAction = {},
            initialTab = MoodTab.JOURNAL,
        )
    }
}
