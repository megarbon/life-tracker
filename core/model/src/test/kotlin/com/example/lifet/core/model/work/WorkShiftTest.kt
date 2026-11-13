package com.example.lifet.core.model.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WorkShiftTest {
    @Test
    fun `net time is elapsed minus breaks`() {
        val shift =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:30",
                breaks = listOf(pause("2026-03-10T13:00", "2026-03-10T13:45")),
            )

        val now = at("2026-03-10T18:00")
        assertEquals(8 * HOUR + 1_800, shift.elapsedSecondsAt(now))
        assertEquals(45 * 60L, shift.breakSecondsAt(now))
        assertEquals(8 * HOUR + 1_800 - 45 * 60, shift.netSecondsAt(now))
    }

    @Test
    fun `an open shift counts up to now and a break in progress stops the clock`() {
        val onBreak =
            shift(
                "a",
                "2026-03-10T09:00",
                null,
                breaks = listOf(pause("2026-03-10T11:00", null)),
            )

        assertEquals(2 * HOUR, onBreak.netSecondsAt(at("2026-03-10T11:00")))
        assertEquals(2 * HOUR, onBreak.netSecondsAt(at("2026-03-10T12:00")))
        assertTrue(onBreak.isOnBreak)
    }

    @Test
    fun `a shift that crosses midnight is a single shift attributed to the day it started`() {
        val night = shift("a", "2026-03-10T22:00", "2026-03-11T06:00")

        assertEquals(day("2026-03-10"), night.startDate)
        assertEquals(8 * HOUR, night.netSecondsAt(at("2026-03-11T12:00")))
        assertTrue(night.crossesMidnightAt(at("2026-03-11T12:00")))
        assertFalse(shift("b", "2026-03-10T09:00", "2026-03-10T17:00").crossesMidnightAt(at("2026-03-11T12:00")))
    }

    @Test
    fun `the spring forward night lasts one hour less in real time`() {
        // En Madrid el 29 de marzo de 2026 las 02:00 pasan a ser las 03:00.
        val night = shift("a", "2026-03-28T22:00", "2026-03-29T06:00")

        assertEquals(7 * HOUR, night.netSecondsAt(at("2026-03-29T12:00")))
    }

    @Test
    fun `the fall back night lasts one hour more in real time`() {
        // El 25 de octubre de 2026 las 03:00 vuelven a ser las 02:00.
        val night = shift("a", "2026-10-24T22:00", "2026-10-25T06:00")

        assertEquals(9 * HOUR, night.netSecondsAt(at("2026-10-25T12:00")))
    }

    @Test
    fun `a break that sticks out of the shift only subtracts the part inside`() {
        val shift =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T10:00",
                breaks = listOf(pause("2026-03-10T09:30", "2026-03-10T11:00")),
            )

        assertEquals(30 * 60L, shift.breakSecondsAt(at("2026-03-10T12:00")))
        assertEquals(30 * 60L, shift.netSecondsAt(at("2026-03-10T12:00")))
    }

    @Test
    fun `an end before the start never produces negative time`() {
        val shift = shift("a", "2026-03-10T09:00", "2026-03-10T08:00")

        assertEquals(0L, shift.netSecondsAt(at("2026-03-10T12:00")))
    }

    @Test
    fun `an open shift is forgotten only after the threshold`() {
        val open = shift("a", "2026-03-10T09:00", null)

        assertFalse(open.isForgotten(at("2026-03-10T20:00")))
        assertFalse(open.isForgotten(at("2026-03-11T01:00")))
        assertTrue(open.isForgotten(at("2026-03-11T01:01")))
        assertFalse(shift("b", "2026-03-10T09:00", "2026-03-10T17:00").isForgotten(at("2026-03-20T09:00")))
    }

    @Test
    fun `starting a break twice or on a closed shift does nothing`() {
        val open = shift("a", "2026-03-10T09:00", null).withBreakStarted(at("2026-03-10T11:00"))

        assertEquals(open, open.withBreakStarted(at("2026-03-10T11:30")))
        val closed = shift("b", "2026-03-10T09:00", "2026-03-10T17:00")
        assertEquals(closed, closed.withBreakStarted(at("2026-03-10T12:00")))
    }

    @Test
    fun `ending a break without one open does nothing and closing the shift closes the break`() {
        val open = shift("a", "2026-03-10T09:00", null)
        assertEquals(open, open.withBreakEnded(at("2026-03-10T12:00")))

        val closed = open.withBreakStarted(at("2026-03-10T11:00")).closedAt(at("2026-03-10T11:30"))
        assertEquals(at("2026-03-10T11:30"), closed.end)
        assertNull(closed.openBreak)
        assertEquals(30 * 60L, closed.breakSecondsAt(at("2026-03-10T12:00")))
    }

    @Test
    fun `the day follows the zone the shift was recorded in`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val shift = shift("a", "2026-03-11T08:00", "2026-03-11T16:00", zone = tokyo)

        assertEquals(day("2026-03-11"), shift.startDate)
        assertEquals(8 * HOUR, shift.netSecondsAt(at("2026-03-12T00:00")))
    }
}
