package com.example.lifet.core.model.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionTest {
    private val session =
        FocusSession(
            id = "f",
            kind = FocusKind.FOCUS,
            start = at("2026-03-12T09:00"),
            zone = MADRID,
            plannedSeconds = 25 * 60L,
        )

    @Test
    fun `remaining time is computed from the start instant, not from ticks`() {
        assertEquals(25 * 60L, session.remainingSecondsAt(at("2026-03-12T09:00")))
        assertEquals(10 * 60L, session.remainingSecondsAt(at("2026-03-12T09:15")))
        assertEquals(0L, session.remainingSecondsAt(at("2026-03-12T15:00")))
    }

    @Test
    fun `elapsed time never exceeds the planned duration or goes negative`() {
        assertEquals(25 * 60L, session.elapsedSecondsAt(at("2026-03-12T20:00")))
        assertEquals(0L, session.elapsedSecondsAt(at("2026-03-12T08:00")))
    }

    @Test
    fun `the timer survives a long time in the background and is due afterwards`() {
        assertFalse(session.isDueAt(at("2026-03-12T09:24")))
        assertTrue(session.isDueAt(at("2026-03-12T09:25")))
        assertTrue(session.isDueAt(at("2026-03-12T12:00")))
    }

    @Test
    fun `a finished session is no longer running or due and keeps its elapsed time`() {
        val done = session.copy(end = at("2026-03-12T09:10"))

        assertFalse(done.isRunning)
        assertFalse(done.isDueAt(at("2026-03-12T12:00")))
        assertEquals(10 * 60L, done.elapsedSecondsAt(at("2026-03-12T12:00")))
    }

    @Test
    fun `progress goes from zero to one`() {
        assertEquals(0f, session.progressAt(at("2026-03-12T09:00")), 0f)
        assertEquals(0.4f, session.progressAt(at("2026-03-12T09:10")), 0.0001f)
        assertEquals(1f, session.progressAt(at("2026-03-12T10:00")), 0f)
        assertEquals(1f, session.copy(plannedSeconds = 0).progressAt(at("2026-03-12T10:00")), 0f)
    }

    @Test
    fun `the default length of each interval matches the pomodoro technique`() {
        assertEquals(25, FocusKind.FOCUS.defaultMinutes)
        assertEquals(5, FocusKind.SHORT_BREAK.defaultMinutes)
        assertEquals(15, FocusKind.LONG_BREAK.defaultMinutes)
        assertTrue(FocusKind.FOCUS.isFocus)
        assertFalse(FocusKind.LONG_BREAK.isFocus)
    }
}
