package com.example.lifet.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetTodayUseCaseTest {
    private val instant = Instant.parse("2026-03-10T23:30:00Z")

    @Test
    fun `returns the date in the clock zone`() {
        val useCase = GetTodayUseCase(Clock.fixed(instant, ZoneId.of("UTC")))

        assertEquals(LocalDate.of(2026, 3, 10), useCase())
    }

    @Test
    fun `the same instant is the next day in a zone ahead of UTC`() {
        val useCase = GetTodayUseCase(Clock.fixed(instant, ZoneId.of("Europe/Madrid")))

        assertEquals(LocalDate.of(2026, 3, 11), useCase())
    }
}
