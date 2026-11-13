package com.example.lifet.core.domain.work

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.FocusTimerAlarm
import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkClockIssue
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.work.InMemoryWorkRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Un repositorio cuya lectura suspende un rato, como una consulta real a Room. Sin él, las pruebas de concurrencia
 * serían secuenciales de hecho (la lectura en memoria es instantánea) y pasarían aunque no hubiera ningún cerrojo.
 */
private class SlowReadRepository(
    private val inner: InMemoryWorkRepository,
) : WorkRepository by inner {
    // La foto se toma antes de esperar, como una consulta que lee y tarda en devolver: dos lecturas simultáneas ven el
    // mismo estado antiguo aunque una de ellas escriba mientras la otra espera.
    override fun observeData(): Flow<WorkDataSet> =
        flow {
            val snapshot = inner.current
            delay(READ_DELAY_MS)
            emit(snapshot)
        }

    private companion object {
        const val READ_DELAY_MS = 50L
    }
}

class WorkConcurrencyTest {
    private val clock = TestClock.fixed("2026-03-12T10:00:00Z")

    private class SilentAlarm : FocusTimerAlarm {
        override fun schedule(session: FocusSession) = Unit

        override fun cancel(sessionId: String) = Unit
    }

    private fun reason(result: AppResult<*>): String? =
        ((result as? AppResult.Failure)?.error as? AppError.Validation)?.reason

    @Test
    fun `two quick taps on clock in create a single shift`() =
        runTest {
            val store = InMemoryWorkRepository()
            val clockActions = ManageWorkClock(SlowReadRepository(store), clock)

            val results = listOf(async { clockActions.clockIn() }, async { clockActions.clockIn() }).awaitAll()

            assertEquals(1, store.current.shifts.size)
            assertEquals(1, results.count { it is AppResult.Success })
            assertEquals(WorkClockIssue.ALREADY_CLOCKED_IN.name, results.mapNotNull(::reason).single())
        }

    @Test
    fun `two quick taps on start create a single focus timer`() =
        runTest {
            val store = InMemoryWorkRepository()
            val timer = ManageFocusTimer(SlowReadRepository(store), SilentAlarm(), clock)

            val results =
                listOf(
                    async { timer.start(FocusKind.FOCUS, 25) },
                    async { timer.start(FocusKind.FOCUS, 25) },
                ).awaitAll()

            assertEquals(1, store.current.focusSessions.size)
            assertEquals(1, results.count { it is AppResult.Success })
            assertEquals(WorkClockIssue.TIMER_ALREADY_RUNNING.name, results.mapNotNull(::reason).single())
        }

    @Test
    fun `finishing expired timers closes every one of them`() =
        runTest {
            val old = Instant.parse("2026-03-12T06:00:00Z")
            val sessions =
                listOf("a", "b").mapIndexed { index, id ->
                    FocusSession(id, FocusKind.FOCUS, old.plusSeconds(index * 60L), clock.zone, 1_500)
                }
            val store = InMemoryWorkRepository(WorkDataSet(focusSessions = sessions))

            ManageFocusTimer(store, SilentAlarm(), clock).finishIfDue()

            assertTrue(store.current.focusSessions.all { !it.isRunning && it.completed })
        }

    @Test
    fun `stopping an interval that already ran out counts it as completed`() =
        runTest {
            val start = Instant.parse("2026-03-12T08:00:00Z")
            val expired = FocusSession("a", FocusKind.FOCUS, start, clock.zone, 1_500)
            val store = InMemoryWorkRepository(WorkDataSet(focusSessions = listOf(expired)))

            ManageFocusTimer(store, SilentAlarm(), clock).stop()

            val saved = store.current.focusSessions.single()
            assertTrue(saved.completed)
            assertEquals(start.plusSeconds(1_500), saved.end)
        }

    @Test
    fun `undoing a delete is rejected if another shift now occupies that time`() =
        runTest {
            val zone = clock.zone

            fun at(hour: String): Instant = Instant.parse("2026-03-11T$hour:00Z")
            val deleted = WorkShift("a", at("08:00"), zone, at("12:00"))
            val replacement = WorkShift("b", at("09:00"), zone, at("11:00"))
            val store = InMemoryWorkRepository(WorkDataSet(shifts = listOf(replacement)))

            val result = ManageWorkClock(store, clock).restoreShift(deleted)

            assertTrue(result is AppResult.Failure)
            assertEquals(listOf("b"), store.current.shifts.map { it.id })
            assertEquals(LocalDate.of(2026, 3, 11), replacement.startDate)
        }
}
