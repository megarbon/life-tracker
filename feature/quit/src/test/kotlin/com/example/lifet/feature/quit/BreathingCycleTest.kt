package com.example.lifet.feature.quit

import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingCycleTest {
    @Test
    fun `a cycle is four seconds in, four held and six out`() {
        assertEquals(14, BreathingCycle.CYCLE_SECONDS)
    }

    @Test
    fun `it starts breathing in with the full phase left`() {
        assertEquals(BreathState(BreathPhase.INHALE, 4, 0), BreathingCycle.stateAt(0))
    }

    @Test
    fun `it moves through the phases at the right seconds`() {
        assertEquals(BreathState(BreathPhase.INHALE, 1, 0), BreathingCycle.stateAt(3))
        assertEquals(BreathState(BreathPhase.HOLD, 4, 0), BreathingCycle.stateAt(4))
        assertEquals(BreathState(BreathPhase.HOLD, 1, 0), BreathingCycle.stateAt(7))
        assertEquals(BreathState(BreathPhase.EXHALE, 6, 0), BreathingCycle.stateAt(8))
        assertEquals(BreathState(BreathPhase.EXHALE, 1, 0), BreathingCycle.stateAt(13))
    }

    @Test
    fun `it loops and counts the completed cycles`() {
        assertEquals(BreathState(BreathPhase.INHALE, 4, 1), BreathingCycle.stateAt(14))
        assertEquals(BreathState(BreathPhase.HOLD, 2, 2), BreathingCycle.stateAt(14 * 2 + 6))
    }

    @Test
    fun `a negative time behaves like the start`() {
        assertEquals(BreathingCycle.stateAt(0), BreathingCycle.stateAt(-7))
    }
}
