package com.example.lifet.core.demo.work

import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.work.ShiftValidator
import com.example.lifet.core.model.work.WorkCalculator
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.work.InMemoryWorkRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WorkDemoDataGeneratorTest {
    // Jueves 12 de marzo de 2026, 12:30 en Madrid.
    private val clock = TestClock.fixed("2026-03-12T11:30:00Z")
    private val now = clock.instant()
    private val zone = clock.zone
    private val today = LocalDate.of(2026, 3, 12)

    private suspend fun load(scenario: DemoScenario): WorkDataSet {
        val repository = InMemoryWorkRepository()
        WorkDemoDataGenerator(repository).seed(scenario, DemoContext(clock))
        return repository.current
    }

    @Test
    fun `area is work`() {
        assertEquals(Area.WORK, WorkDemoDataGenerator(InMemoryWorkRepository()).area)
    }

    @Test
    fun `empty scenario leaves nothing even after a regular load`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val generator = WorkDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.seed(DemoScenario.EMPTY, DemoContext(clock))

            assertTrue(repository.current.hasNoRecords)
            assertTrue(repository.current.projects.isEmpty())
        }

    @Test
    fun `clear removes everything`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val generator = WorkDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.clear()

            assertTrue(repository.current.hasNoRecords)
            assertTrue(repository.current.projects.isEmpty())
        }

    @Test
    fun `seeding twice gives the same data`() =
        runTest {
            assertEquals(load(DemoScenario.REGULAR), load(DemoScenario.REGULAR))
            assertEquals(load(DemoScenario.EDGE_CASES), load(DemoScenario.EDGE_CASES))
            assertEquals(load(DemoScenario.NEW_USER), load(DemoScenario.NEW_USER))
        }

    @Test
    fun `no scenario puts anything in the future`() =
        runTest {
            DemoScenario.entries.forEach { scenario ->
                val data = load(scenario)
                assertTrue("$scenario", data.shifts.none { it.start > now })
                assertTrue("$scenario", data.shifts.none { (it.end ?: now) > now })
                assertTrue("$scenario", data.shifts.flatMap { it.breaks }.none { (it.end ?: now) > now })
                assertTrue("$scenario", data.focusSessions.none { it.start > now })
                assertTrue("$scenario", data.notes.none { it.date > today })
            }
        }

    @Test
    fun `new user has about three days of data with today's shift in progress`() =
        runTest {
            val data = load(DemoScenario.NEW_USER)
            val summary = WorkCalculator(data, now, zone).todaySummary()

            assertEquals(setOf(today.minusDays(2), today.minusDays(1), today), data.shifts.map { it.startDate }.toSet())
            assertTrue(summary.isClockedIn)
            assertTrue(data.projects.isNotEmpty())
        }

    @Test
    fun `regular covers about six months from monday to friday with three projects`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val first = data.shifts.minOf { it.startDate }

            assertTrue("empieza hace al menos 150 días", first <= today.minusDays(150))
            assertEquals(3, data.projects.size)
            val weekdays = data.shifts.filter { it.startDate.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
            assertTrue("hay mucha más actividad entre semana", weekdays.size > data.shifts.size * 3 / 4)
            assertTrue(data.shifts.all { it.projectId == null || it.projectId in data.projects.map { p -> p.id } })
        }

    @Test
    fun `regular has exactly two forgotten clock-outs that can be closed afterwards`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val forgotten = data.shifts.filter { it.isForgotten(now) }

            assertEquals(2, forgotten.size)
            assertTrue("son de hace semanas", forgotten.all { it.startDate < today.minusDays(14) })
            assertTrue(WorkCalculator(data, now, zone).todaySummary().needsClosing)

            // Cerrar a posteriori una de ellas a las 18:00 del mismo día es válido y desaparece del aviso.
            val shift = forgotten.first()
            val closed =
                shift.closedAt(
                    shift.start
                        .atZone(zone)
                        .toLocalDate()
                        .atTime(18, 0)
                        .atZone(zone)
                        .toInstant(),
                )
            val others = data.shifts.filter { it.id != shift.id }
            assertTrue(ShiftValidator.validate(closed, others, now).isEmpty())
            val after = data.copy(shifts = others + closed)
            assertEquals(1, WorkCalculator(after, now, zone).forgotten().size)
        }

    @Test
    fun `regular has exactly one shift that crosses midnight`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val crossing = data.shifts.filter { !it.isOpen && it.crossesMidnightAt(now) }

            assertEquals(1, crossing.size)
            val night = crossing.single()
            assertEquals(day(night.start, night.zone), night.startDate)
            // Cuenta una sola vez, en el día en que empezó.
            val series =
                WorkCalculator(data, now, zone).series(
                    DateRange.containing(
                        DateRangeUnit.DAY,
                        night.startDate,
                    ),
                )
            assertEquals(night.netSecondsAt(now), series.days.single().workedSeconds - otherHoursOn(data, night))
        }

    @Test
    fun `regular has days that vary and a week of holidays with no shifts`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val lengths =
                data.shifts
                    .filter {
                        !it.isOpen &&
                            !it.crossesMidnightAt(
                                now,
                            )
                    }.map { it.netSecondsAt(now) }
                    .toSet()

            assertTrue("las jornadas no son todas iguales", lengths.size > 5)
            val mondays = data.shifts.map { it.startDate }.toSet()
            val weeksWithout =
                (1L..26L).count { back ->
                    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong()).minusWeeks(back)
                    (0L..4L).none { monday.plusDays(it) in mondays }
                }
            assertEquals(1, weeksWithout)
        }

    @Test
    fun `regular produces a believable balance and every shift is valid`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val summary = WorkCalculator(data, now, zone).todaySummary()

            assertTrue(summary.hasData)
            assertTrue(
                "el saldo acumulado es razonable",
                summary.totalBalanceSeconds in (-200 * 3_600L)..(200 * 3_600L),
            )
            data.shifts.forEach { shift ->
                val issues = ShiftValidator.validate(shift, data.shifts, now)
                assertTrue("${shift.id}: $issues", issues.isEmpty() || shift.isForgotten(now))
            }
        }

    @Test
    fun `regular includes focus sessions and day notes`() =
        runTest {
            val data = load(DemoScenario.REGULAR)

            assertTrue(data.focusSessions.size >= 20)
            assertTrue(data.focusSessions.all { it.completed && it.kind.isFocus })
            assertTrue(data.notes.size >= 10)
        }

    @Test
    fun `edge cases cross the DST changes and keep real durations`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)
            val spring = data.shifts.single { it.id == "edge-dst-spring" }
            val autumn = data.shifts.single { it.id == "edge-dst-autumn" }

            assertEquals(7 * 3_600L, spring.netSecondsAt(now))
            assertEquals(9 * 3_600L, autumn.netSecondsAt(now))
            assertTrue(spring.crossesMidnightAt(now) && autumn.crossesMidnightAt(now))
        }

    @Test
    fun `edge cases have a 24 hour shift and twenty breaks and a shift in another zone`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)

            assertTrue(data.shifts.any { it.id == "edge-24h" && it.netSecondsAt(now) == WorkRules.MAX_SHIFT_SECONDS })
            assertTrue(data.shifts.any { it.breaks.size == WorkRules.MAX_BREAKS })
            assertTrue(data.shifts.any { it.zone.id == "Asia/Tokyo" })
        }

    @Test
    fun `edge cases keep very long texts with emojis inside the limits the form accepts`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)

            assertTrue(data.notes.any { it.text.length > WorkRules.DAY_NOTE_MAX_LENGTH - 100 && "📝" in it.text })
            assertTrue(data.notes.all { it.text.length <= WorkRules.DAY_NOTE_MAX_LENGTH })
            assertTrue(data.shifts.all { it.note.length <= WorkRules.SHIFT_NOTE_MAX_LENGTH })
            assertTrue(data.projects.any { it.name.length > WorkRules.PROJECT_NAME_MAX_LENGTH - 50 && "🌍" in it.name })
            assertTrue(data.projects.all { it.name.length <= WorkRules.PROJECT_NAME_MAX_LENGTH })
            assertTrue(data.focusSessions.all { it.title.length <= WorkRules.FOCUS_TITLE_MAX_LENGTH })
        }

    @Test
    fun `edge cases only contain shifts the validator accepts`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)

            data.shifts.forEach { shift ->
                assertEquals("${shift.id}", emptySet<Any>(), ShiftValidator.validate(shift, data.shifts, now))
            }
            assertTrue(data.shifts.all { it.durationFits() })
        }

    @Test
    fun `edge cases include a shift in progress with an open break and a day gap`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)
            val summary = WorkCalculator(data, now, zone).todaySummary()

            assertTrue(summary.isClockedIn)
            assertTrue(data.shifts.any { it.isOnBreak })
            val busy = data.shifts.map { it.startDate }.toSet()
            assertTrue((4L..11L).none { today.minusDays(it) in busy })
            assertEquals(37 * 60 + 30, data.settings.weeklyTargetMinutes)
        }

    private fun day(
        instant: Instant,
        zone: ZoneId,
    ): LocalDate = instant.atZone(zone).toLocalDate()

    private fun otherHoursOn(
        data: WorkDataSet,
        night: WorkShift,
    ): Long =
        data.shifts
            .filter { it.id != night.id && it.startDate == night.startDate && !it.isForgotten(now) }
            .sumOf { it.netSecondsAt(now) }

    private fun WorkShift.durationFits(): Boolean = elapsedSecondsAt(now) <= WorkRules.MAX_SHIFT_SECONDS
}
