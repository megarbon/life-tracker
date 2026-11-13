package com.example.lifet.core.domain

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Reloj que avanza con el tiempo virtual del test, para comprobar el cambio de día sin esperar de verdad. */
@OptIn(ExperimentalCoroutinesApi::class)
private class VirtualClock(
    private val scope: TestScope,
    private val start: Instant,
    private val zone: ZoneId,
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = VirtualClock(scope, start, zone)

    override fun instant(): Instant = start.plusMillis(scope.currentTime)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTodayTest {
    @Test
    fun `emits today immediately`() =
        runTest {
            val clock = VirtualClock(this, Instant.parse("2026-03-10T09:00:00Z"), ZoneOffset.UTC)

            ObserveToday(clock)().test {
                assertEquals(LocalDate.of(2026, 3, 10), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits the next day once midnight passes without anyone touching the screen`() =
        runTest {
            // 23:50 UTC: faltan 10 minutos para el día siguiente.
            val clock = VirtualClock(this, Instant.parse("2026-03-10T23:50:00Z"), ZoneOffset.UTC)

            ObserveToday(clock)().test {
                assertEquals(LocalDate.of(2026, 3, 10), awaitItem())
                assertEquals(LocalDate.of(2026, 3, 11), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `midnight is computed in the clock zone, not in UTC`() =
        runTest {
            // 22:30 UTC del día 10 son las 23:30 en Madrid (invierno, UTC+1): sigue siendo día 10 y falta media hora.
            val madrid = ZoneId.of("Europe/Madrid")
            val clock = VirtualClock(this, Instant.parse("2026-01-10T22:30:00Z"), madrid)

            ObserveToday(clock)().test {
                assertEquals(LocalDate.of(2026, 1, 10), awaitItem())
                assertEquals(LocalDate.of(2026, 1, 11), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
