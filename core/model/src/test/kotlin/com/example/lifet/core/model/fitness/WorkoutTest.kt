package com.example.lifet.core.model.fitness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class WorkoutTest {
    @Test
    fun `epley estimate uses load times one plus reps over thirty`() {
        assertEquals(116_666L, StrengthSet(reps = 5, weightGrams = 100_000).estimatedOneRepMaxGrams)
    }

    @Test
    fun `a single rep is the load itself and bodyweight has no estimate`() {
        assertEquals(80_000L, StrengthSet(reps = 1, weightGrams = 80_000).estimatedOneRepMaxGrams)
        assertNull(StrengthSet(reps = 12, weightGrams = 0).estimatedOneRepMaxGrams)
    }

    @Test
    fun `volume multiplies reps by load across sets and exercises`() {
        val workout =
            strength(
                "w",
                TODAY,
                lift("Press banca", 5 to 60_000L, 5 to 60_000L),
                lift("Remo", 10 to 40_000L),
            )

        assertEquals(60_000L * 10 + 40_000L * 10, workout.volumeGrams)
        assertEquals(3, workout.totalSets)
    }

    @Test
    fun `a workout inside one day counts all its seconds on that day`() {
        val workout = cardio("a", TODAY, minutes = 45)

        assertEquals(mapOf(TODAY to 45 * 60L), workout.secondsByDay())
    }

    @Test
    fun `a workout crossing midnight is split between both days`() {
        val workout = cardio("a", TODAY, minutes = 120, time = LocalTime.of(22, 30))

        assertEquals(
            mapOf(TODAY to 90 * 60L, TODAY.plusDays(1) to 30 * 60L),
            workout.secondsByDay(),
        )
    }

    @Test
    fun `a workout over the spring forward change splits by real elapsed time`() {
        // 28 de marzo de 2026 23:30 (CET) + 3 h reales = 03:30 (CEST) del día 29.
        val workout = cardio("dst", LocalDate.of(2026, 3, 28), minutes = 180, time = LocalTime.of(23, 30))

        val byDay = workout.secondsByDay()

        assertEquals(30 * 60L, byDay[LocalDate.of(2026, 3, 28)])
        assertEquals(150 * 60L, byDay[LocalDate.of(2026, 3, 29)])
        assertEquals(180 * 60L, byDay.values.sum())
    }

    @Test
    fun `start date follows the zone stored with the workout`() {
        val workout =
            Workout(
                id = "tokyo",
                type = WorkoutType.RUN,
                title = "",
                start = Instant.parse("2026-03-10T16:00:00Z"),
                zone = java.time.ZoneId.of("Asia/Tokyo"),
                durationSec = 1_800,
            )

        assertEquals(LocalDate.of(2026, 3, 11), workout.startDate)
    }

    @Test
    fun `a zero length workout has no active seconds`() {
        assertTrue(cardio("zero", TODAY, minutes = 0).secondsByDay().isEmpty())
    }
}
