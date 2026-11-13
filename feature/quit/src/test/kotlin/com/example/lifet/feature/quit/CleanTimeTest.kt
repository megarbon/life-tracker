package com.example.lifet.feature.quit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CleanTimeTest {
    @Test
    fun `a negative duration is clamped to zero`() {
        assertEquals(CleanTime(0, 0, 0, 0, 0), CleanTime.of(-500))
    }

    @Test
    fun `seconds break down into days, hours, minutes and seconds`() {
        val time = CleanTime.of(2 * 86_400L + 3 * 3_600L + 4 * 60L + 5)

        assertEquals(CleanTime(0, 2, 3, 4, 5), time)
        assertEquals(2L, time.totalDays)
    }

    @Test
    fun `years are 365 days and the rest are days`() {
        val time = CleanTime.of((365L * 6 + 40) * 86_400L)

        assertEquals(6L, time.years)
        assertEquals(40L, time.days)
        assertEquals(365L * 6 + 40, time.totalDays)
    }

    @Test
    fun `seconds are shown only during the first day`() {
        assertTrue(CleanTime.of(86_399).showsSeconds)
        assertFalse(CleanTime.of(86_400).showsSeconds)
        assertEquals(1_000L, CleanTime.of(10).tickMillis)
        assertEquals(60_000L, CleanTime.of(2 * 86_400L).tickMillis)
    }

    @Test
    fun `between two instants never goes negative`() {
        val since = Instant.parse("2026-03-10T10:00:00Z")

        assertEquals(CleanTime.of(0), CleanTime.between(since, since.minusSeconds(30)))
        assertEquals(CleanTime.of(90), CleanTime.between(since, since.plusSeconds(90)))
    }

    @Test
    fun `the next tick lines up with the second of the streak during the first day`() {
        val since = Instant.parse("2026-03-10T10:00:00Z")

        // A 0,25 s del siguiente segundo falta 0,75 s para que cambie el dígito.
        assertEquals(750L, millisUntilNextTick(since, since.plusMillis(5_250)))
        assertEquals(1_000L, millisUntilNextTick(since, since.plusMillis(5_000)))
    }

    @Test
    fun `after the first day the tick lines up with the minute of the streak`() {
        val since = Instant.parse("2026-03-10T10:00:00Z")

        val twoDaysAnd20s = since.plusSeconds(2 * 86_400L + 20)

        assertEquals(40_000L, millisUntilNextTick(since, twoDaysAnd20s))
    }

    @Test
    fun `a streak that has not started yet waits a whole tick`() {
        val since = Instant.parse("2026-03-10T10:00:00Z")

        assertEquals(1_000L, millisUntilNextTick(since, since.minusSeconds(60)))
    }
}
