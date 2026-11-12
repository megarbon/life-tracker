package com.example.lifet.core.model.mood

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MoodStatsTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.parse("2026-03-10")

    private fun entry(
        date: LocalDate,
        level: MoodLevel,
        time: LocalTime = LocalTime.NOON,
        emotions: List<String> = emptyList(),
        factors: List<String> = emptyList(),
    ) = MoodEntry(
        instant = date.atTime(time).atZone(zone).toInstant(),
        zone = zone,
        level = level,
        emotions = emotions,
        factors = factors,
    )

    @Test
    fun `local date uses the stored zone, not UTC`() {
        // 00:30 en Madrid es 23:30 UTC del día anterior: el día lógico es el de la zona del usuario.
        val lateNight = entry(today, MoodLevel.GOOD, LocalTime.of(0, 30))

        assertEquals(today, lateNight.localDate)
    }

    @Test
    fun `every day weighs the same in the average regardless of check-ins`() {
        val entries =
            listOf(
                entry(today, MoodLevel.VERY_GOOD, LocalTime.of(9, 0)),
                entry(today, MoodLevel.VERY_GOOD, LocalTime.of(13, 0)),
                entry(today, MoodLevel.VERY_GOOD, LocalTime.of(20, 0)),
                entry(today.minusDays(1), MoodLevel.VERY_LOW),
            )

        assertEquals(3.0, MoodStats.averageOfDays(entries) ?: 0.0, DELTA)
    }

    @Test
    fun `moving average skips days without data`() {
        val daily = mapOf(today to 4.0, today.minusDays(2) to 2.0, today.minusDays(10) to 1.0)

        assertEquals(3.0, MoodStats.movingAverage(daily, today) ?: 0.0, DELTA)
        assertNull(MoodStats.movingAverage(daily, today.minusDays(20)))
    }

    @Test
    fun `series covers every day of the range and uses previous days for the moving average`() {
        val range = DateRange.containing(DateRangeUnit.WEEK, today)
        val beforeRange = entry(range.start.minusDays(1), MoodLevel.VERY_LOW)
        val entries = listOf(beforeRange, entry(range.start, MoodLevel.VERY_GOOD))

        val series = MoodStats.series(range, entries)

        assertEquals(7, series.days.size)
        assertEquals(5.0, series.days.first().average ?: 0.0, DELTA)
        assertEquals(3.0, series.days.first().movingAverage ?: 0.0, DELTA)
        assertEquals(1, series.entryCount)
        assertEquals(5.0, series.average ?: 0.0, DELTA)
    }

    @Test
    fun `top emotions are sorted by frequency then alphabetically`() {
        val entries =
            listOf(
                entry(today, MoodLevel.GOOD, emotions = listOf("joy", "calm")),
                entry(today, MoodLevel.GOOD, emotions = listOf("joy", "tired")),
                entry(today, MoodLevel.GOOD, emotions = listOf("anxiety")),
            )

        val top = MoodStats.topEmotions(entries, limit = 3)

        assertEquals(listOf("joy", "anxiety", "calm"), top.map { it.emotion })
        assertEquals(2, top.first().count)
    }

    @Test
    fun `factor impact compares days with and without the factor`() {
        val entries =
            (0L until 6L).map { offset ->
                val sport = offset % 2 == 0L
                entry(
                    today.minusDays(offset),
                    if (sport) MoodLevel.VERY_GOOD else MoodLevel.NEUTRAL,
                    factors = if (sport) listOf("sport") else listOf("work"),
                )
            }

        val impacts = MoodStats.factorImpacts(entries)
        val sport = impacts.first { it.factor == "sport" }

        assertEquals(5.0, sport.averageWith, DELTA)
        assertEquals(3.0, sport.averageWithout, DELTA)
        assertEquals(2.0, sport.delta, DELTA)
        assertEquals(3, sport.daysWith)
    }

    @Test
    fun `factor impact needs enough days on both sides`() {
        val entries = listOf(entry(today, MoodLevel.GOOD, factors = listOf("sport")), entry(today, MoodLevel.LOW))

        assertTrue(MoodStats.factorImpacts(entries).isEmpty())
    }

    @Test
    fun `low streak counts from yesterday when today has no check-in and breaks on gaps`() {
        val daily =
            (1L..5L).associate { today.minusDays(it) to 2.0 } +
                mapOf(today.minusDays(7) to 1.0)

        assertEquals(5, MoodStats.lowStreak(daily, today))
        assertEquals(0, MoodStats.lowStreak(daily + (today to 4.0), today))
    }

    @Test
    fun `today summary reports missing check-in and support flag`() {
        val entries = (1L..5L).map { entry(today.minusDays(it), MoodLevel.LOW) }

        val summary = MoodStats.todaySummary(entries, today)

        assertTrue(summary.missingCheckIn)
        assertNull(summary.averageToday)
        assertTrue(summary.showSupport)
        assertEquals(2.0, summary.weekAverage ?: 0.0, DELTA)
    }

    @Test
    fun `today summary averages today's check-ins and keeps the last level`() {
        val entries =
            listOf(
                entry(today, MoodLevel.GOOD, LocalTime.of(20, 0)),
                entry(today, MoodLevel.NEUTRAL, LocalTime.of(8, 0)),
            )

        val summary = MoodStats.todaySummary(entries, today)

        assertFalse(summary.missingCheckIn)
        assertEquals(2, summary.checkInsToday)
        assertEquals(3.5, summary.averageToday ?: 0.0, DELTA)
        assertEquals(MoodLevel.GOOD, summary.lastLevel)
        assertFalse(summary.showSupport)
    }

    @Test
    fun `tags are normalized`() {
        assertEquals("yoga matinal", MoodTags.normalize("  yoga,   matinal "))
        assertNull(MoodTags.normalize(" , "))
        assertEquals(MoodTags.MAX_LENGTH, MoodTags.normalize("x".repeat(100))?.length)
    }

    @Test
    fun `mood level maps from value and rejects out of range`() {
        assertEquals(MoodLevel.VERY_GOOD, MoodLevel.fromValue(5))
        assertNull(MoodLevel.fromValue(0))
    }

    private companion object {
        const val DELTA = 0.0001
    }
}
