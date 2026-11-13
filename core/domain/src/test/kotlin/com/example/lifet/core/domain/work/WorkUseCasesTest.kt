package com.example.lifet.core.domain.work

import app.cash.turbine.test
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.FocusTimerAlarm
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkClockIssue
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.core.testing.work.InMemoryWorkRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WorkUseCasesTest {
    private val zone = ZoneId.of("Europe/Madrid")

    private fun at(time: String): Instant = LocalDateTime.parse(time).atZone(zone).toInstant()

    private fun clockAt(time: String): Clock = Clock.fixed(at(time), zone)

    private fun shift(
        id: String,
        start: String,
        end: String?,
        breaks: List<WorkBreak> = emptyList(),
    ) = WorkShift(id, at(start), zone, end?.let(::at), breaks)

    private fun reason(result: AppResult<*>): String =
        ((result as AppResult.Failure).error as AppError.Validation).reason

    private class RecordingAlarm : FocusTimerAlarm {
        val scheduled = mutableListOf<String>()
        val cancelled = mutableListOf<String>()

        override fun schedule(session: FocusSession) {
            scheduled += session.id
        }

        override fun cancel(sessionId: String) {
            cancelled += sessionId
        }
    }

    // ---- Resumen de Hoy ----

    @Test
    fun `today summary of an unused area has no data`() =
        runTest {
            ObserveWorkTodaySummary(InMemoryWorkRepository(), clockAt("2026-03-12T12:00"))().test {
                val summary = awaitItem()
                assertFalse(summary.hasData)
                assertFalse(summary.isClockedIn)
                assertEquals(40 * 3_600L, summary.weeklyTargetSeconds)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `today summary reacts when clocking in and out`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val clock = clockAt("2026-03-12T09:00")

            ObserveWorkTodaySummary(repository, clock)().test {
                assertFalse(awaitItem().isClockedIn)
                ManageWorkClock(repository, clock).clockIn()
                val clockedIn = awaitItem()
                assertTrue(clockedIn.isClockedIn)
                assertEquals(clock.instant(), clockedIn.shiftStart)

                ManageWorkClock(repository, clockAt("2026-03-12T17:00")).clockOut()
                val done = awaitItem()
                assertFalse(done.isClockedIn)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `today summary does not emit again when nothing it shows changed`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-12T08:00", "2026-03-12T09:00"))))

            ObserveWorkTodaySummary(repository, clockAt("2026-03-12T12:00"))().test {
                awaitItem()
                repository.saveProject(
                    com.example.lifet.core.model.work
                        .WorkProject("p", "Web", ProjectColor.BLUE),
                )
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `series fills the days up to today with zeros`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-10T09:00", "2026-03-10T17:00"))))
            val week = DateRange.containing(DateRangeUnit.WEEK, LocalDate.of(2026, 3, 12))

            ObserveWorkSeries(repository, clockAt("2026-03-12T12:00"))(week).test {
                val series = awaitItem()
                assertEquals(listOf(0L, 8 * 3_600L, 0L, 0L), series.days.map { it.workedSeconds })
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Fichaje ----

    @Test
    fun `clock in then break then resume then out records the whole day`() =
        runTest {
            val repository = InMemoryWorkRepository()
            ManageWorkClock(repository, clockAt("2026-03-12T09:00")).clockIn(projectId = "p")
            ManageWorkClock(repository, clockAt("2026-03-12T13:00")).startBreak()
            ManageWorkClock(repository, clockAt("2026-03-12T13:30")).endBreak()
            val out = ManageWorkClock(repository, clockAt("2026-03-12T17:30")).clockOut()

            val shift = (out as AppResult.Success).value
            assertEquals(at("2026-03-12T17:30"), shift.end)
            assertEquals("p", shift.projectId)
            assertEquals(1, shift.breaks.size)
            assertEquals(8 * 3_600L, shift.netSecondsAt(at("2026-03-12T18:00")))
            assertEquals(1, repository.current.shifts.size)
        }

    @Test
    fun `clocking in twice is rejected`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T09:00"))
            clock.clockIn()

            assertEquals(WorkClockIssue.ALREADY_CLOCKED_IN.name, reason(clock.clockIn()))
            assertEquals(1, repository.current.shifts.size)
        }

    @Test
    fun `clocking out, breaking or resuming with nothing in progress is rejected`() =
        runTest {
            val clock = ManageWorkClock(InMemoryWorkRepository(), clockAt("2026-03-12T09:00"))

            assertEquals(WorkClockIssue.NOT_CLOCKED_IN.name, reason(clock.clockOut()))
            assertEquals(WorkClockIssue.NOT_CLOCKED_IN.name, reason(clock.startBreak()))
            assertEquals(WorkClockIssue.NOT_CLOCKED_IN.name, reason(clock.endBreak()))
        }

    @Test
    fun `starting a second break or resuming without one is rejected`() =
        runTest {
            val repository = InMemoryWorkRepository()
            ManageWorkClock(repository, clockAt("2026-03-12T09:00")).clockIn()
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T11:00"))

            assertEquals(WorkClockIssue.NOT_ON_BREAK.name, reason(clock.endBreak()))
            assertTrue(clock.startBreak() is AppResult.Success)
            assertEquals(WorkClockIssue.ALREADY_ON_BREAK.name, reason(clock.startBreak()))
        }

    @Test
    fun `clocking out while on a break closes the break too`() =
        runTest {
            val repository = InMemoryWorkRepository()
            ManageWorkClock(repository, clockAt("2026-03-12T09:00")).clockIn()
            ManageWorkClock(repository, clockAt("2026-03-12T12:00")).startBreak()

            val out = ManageWorkClock(repository, clockAt("2026-03-12T12:30")).clockOut() as AppResult.Success

            assertNull(out.value.openBreak)
            assertEquals(
                at("2026-03-12T12:30"),
                out.value.breaks
                    .single()
                    .end,
            )
        }

    @Test
    fun `a forgotten shift can be closed afterwards with the real exit time`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-09T09:00", null))))
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T09:00"))

            val result = clock.closeAt("a", at("2026-03-09T18:15"))

            assertTrue(result is AppResult.Success)
            assertEquals(
                at("2026-03-09T18:15"),
                repository.current.shifts
                    .single()
                    .end,
            )
            ObserveWorkTodaySummary(repository, clockAt("2026-03-12T12:00"))().test {
                assertFalse(awaitItem().needsClosing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `closing a forgotten shift before its entry or in the future is rejected and nothing is written`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-09T09:00", null))))
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T09:00"))

            assertTrue(ShiftIssueNames.endBeforeStart in reason(clock.closeAt("a", at("2026-03-09T08:00"))))
            assertTrue(ShiftIssueNames.endInFuture in reason(clock.closeAt("a", at("2026-03-13T08:00"))))
            assertTrue(ShiftIssueNames.tooLong in reason(clock.closeAt("a", at("2026-03-11T09:00"))))
            assertNull(
                repository.current.shifts
                    .single()
                    .end,
            )
        }

    @Test
    fun `closing a shift that does not exist or is already closed reports it`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-09T09:00", "2026-03-09T17:00"))))
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T09:00"))

            assertEquals(WorkClockIssue.SHIFT_NOT_FOUND.name, reason(clock.closeAt("a", at("2026-03-09T18:00"))))
            assertEquals(WorkClockIssue.SHIFT_NOT_FOUND.name, reason(clock.closeAt("zzz", at("2026-03-09T18:00"))))
        }

    @Test
    fun `overlapping breaks are rejected by validation and nothing is saved`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T20:00"))
            val overlapping =
                shift(
                    "a",
                    "2026-03-12T09:00",
                    "2026-03-12T17:00",
                    breaks =
                        listOf(
                            WorkBreak(at("2026-03-12T12:00"), at("2026-03-12T13:00")),
                            WorkBreak(at("2026-03-12T12:30"), at("2026-03-12T13:30")),
                        ),
                )

            val result = clock.saveShift(overlapping)

            assertTrue(ShiftIssueNames.breaksOverlap in reason(result))
            assertTrue(repository.current.shifts.isEmpty())
        }

    @Test
    fun `a manual shift that overlaps another is rejected but editing the same shift is fine`() =
        runTest {
            val existing = shift("a", "2026-03-12T09:00", "2026-03-12T13:00")
            val repository = InMemoryWorkRepository(WorkDataSet(shifts = listOf(existing)))
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T20:00"))

            val overlap = clock.saveShift(shift("b", "2026-03-12T12:00", "2026-03-12T15:00"))
            val edit = clock.saveShift(existing.copy(end = at("2026-03-12T14:00")))

            assertTrue(ShiftIssueNames.overlaps in reason(overlap))
            assertTrue(edit is AppResult.Success)
            assertEquals(1, repository.current.shifts.size)
        }

    @Test
    fun `a manual shift that crosses midnight and the DST change is accepted`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val clock = ManageWorkClock(repository, clockAt("2026-03-30T12:00"))

            val result = clock.saveShift(shift("a", "2026-03-28T22:00", "2026-03-29T06:00"))

            assertTrue(result is AppResult.Success)
            assertEquals(
                7 * 3_600L,
                repository.current.shifts
                    .single()
                    .netSecondsAt(at("2026-03-30T12:00")),
            )
        }

    @Test
    fun `deleting a shift offers undo by restoring the same shift`() =
        runTest {
            val original = shift("a", "2026-03-12T09:00", "2026-03-12T13:00")
            val repository = InMemoryWorkRepository(WorkDataSet(shifts = listOf(original)))
            val clock = ManageWorkClock(repository, clockAt("2026-03-12T20:00"))

            clock.deleteShift("a")
            assertTrue(repository.current.shifts.isEmpty())
            clock.restoreShift(original)

            assertEquals(listOf(original), repository.current.shifts)
        }

    @Test
    fun `a failing write surfaces as a storage error`() =
        runTest {
            val repository = InMemoryWorkRepository().apply { failWrites = true }

            val result = ManageWorkClock(repository, clockAt("2026-03-12T09:00")).clockIn()

            assertEquals(AppError.Storage, (result as AppResult.Failure).error)
        }

    // ---- Temporizador de foco ----

    @Test
    fun `starting a focus session stores its start instant and schedules the alarm`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val alarm = RecordingAlarm()

            val result =
                ManageFocusTimer(
                    repository,
                    alarm,
                    clockAt("2026-03-12T10:00"),
                ).start(FocusKind.FOCUS, 25, title = " Informe ")

            val session = (result as AppResult.Success).value
            assertEquals(at("2026-03-12T10:00"), session.start)
            assertEquals(25 * 60L, session.plannedSeconds)
            assertEquals("Informe", session.title)
            assertEquals(listOf(session.id), alarm.scheduled)
            assertEquals(listOf(session), repository.current.focusSessions)
        }

    @Test
    fun `the timer survives a process death because a new instance recomputes from the stored start`() =
        runTest {
            val repository = InMemoryWorkRepository()
            ManageFocusTimer(repository, RecordingAlarm(), clockAt("2026-03-12T10:00")).start(FocusKind.FOCUS, 25)

            // "Proceso nuevo": otro ViewModel, otro reloj, 10 minutos después, mismo almacenamiento.
            val later = clockAt("2026-03-12T10:10")
            val running =
                ObserveWorkOverview(repository, later)().let { flow ->
                    var seen: FocusSession? = null
                    flow.test {
                        seen = awaitItem().focus.running
                        cancelAndIgnoreRemainingEvents()
                    }
                    seen
                }

            assertNotNull(running)
            assertEquals(15 * 60L, running?.remainingSecondsAt(later.instant()))
        }

    @Test
    fun `a second timer cannot start while one is running`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val timer = ManageFocusTimer(repository, RecordingAlarm(), clockAt("2026-03-12T10:00"))
            timer.start(FocusKind.FOCUS, 25)

            assertEquals(WorkClockIssue.TIMER_ALREADY_RUNNING.name, reason(timer.start(FocusKind.FOCUS, 25)))
            assertEquals(1, repository.current.focusSessions.size)
        }

    @Test
    fun `an expired timer closes at its planned end as completed and frees the next one`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val alarm = RecordingAlarm()
            val started =
                ManageFocusTimer(
                    repository,
                    alarm,
                    clockAt("2026-03-12T10:00"),
                ).start(FocusKind.FOCUS, 25) as AppResult.Success

            // La app estuvo 2 horas en segundo plano.
            val later = ManageFocusTimer(repository, alarm, clockAt("2026-03-12T12:00"))
            val finished = later.finishIfDue()

            assertNotNull(finished)
            assertEquals(at("2026-03-12T10:25"), finished?.end)
            assertTrue(finished?.completed == true)
            assertEquals(listOf(started.value.id), alarm.cancelled)
            assertNull(later.finishIfDue())
            assertTrue(later.start(FocusKind.SHORT_BREAK, 5) is AppResult.Success)
        }

    @Test
    fun `starting a timer also closes an expired one left over`() =
        runTest {
            val repository = InMemoryWorkRepository()
            ManageFocusTimer(repository, RecordingAlarm(), clockAt("2026-03-12T10:00")).start(FocusKind.FOCUS, 25)

            val result =
                ManageFocusTimer(
                    repository,
                    RecordingAlarm(),
                    clockAt("2026-03-12T11:00"),
                ).start(FocusKind.FOCUS, 25)

            assertTrue(result is AppResult.Success)
            assertEquals(2, repository.current.focusSessions.size)
            assertEquals(1, repository.current.focusSessions.count { it.completed })
        }

    @Test
    fun `stopping early keeps what was done and stopping at once discards it`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val alarm = RecordingAlarm()
            ManageFocusTimer(repository, alarm, clockAt("2026-03-12T10:00")).start(FocusKind.FOCUS, 25)

            ManageFocusTimer(repository, alarm, clockAt("2026-03-12T10:10")).stop()
            val kept = repository.current.focusSessions.single()
            assertFalse(kept.completed)
            assertEquals(at("2026-03-12T10:10"), kept.end)

            val accidental = ManageFocusTimer(repository, alarm, clockAt("2026-03-12T11:00"))
            accidental.start(FocusKind.FOCUS, 25)
            ManageFocusTimer(
                repository,
                alarm,
                clockAt("2026-03-12T11:00").let {
                    Clock.offset(it, java.time.Duration.ofSeconds(20))
                },
            ).stop()
            assertEquals(1, repository.current.focusSessions.size)
        }

    @Test
    fun `stopping with no timer running is rejected and invalid durations too`() =
        runTest {
            val timer = ManageFocusTimer(InMemoryWorkRepository(), RecordingAlarm(), clockAt("2026-03-12T10:00"))

            assertEquals(WorkClockIssue.NO_TIMER_RUNNING.name, reason(timer.stop()))
            assertEquals(WorkClockIssue.INVALID_DURATION.name, reason(timer.start(FocusKind.FOCUS, 0)))
            assertEquals(WorkClockIssue.INVALID_DURATION.name, reason(timer.start(FocusKind.FOCUS, 241)))
            assertEquals(
                WorkClockIssue.TITLE_TOO_LONG.name,
                reason(timer.start(FocusKind.FOCUS, 25, title = "x".repeat(121))),
            )
        }

    @Test
    fun `a break interval ignores project and title`() =
        runTest {
            val repository = InMemoryWorkRepository()

            ManageFocusTimer(repository, RecordingAlarm(), clockAt("2026-03-12T10:00"))
                .start(FocusKind.SHORT_BREAK, 5, projectId = "p", title = "no")

            val session = repository.current.focusSessions.single()
            assertNull(session.projectId)
            assertEquals("", session.title)
        }

    // ---- Proyectos, notas y ajustes ----

    @Test
    fun `projects are created trimmed, edited in place and validated`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val data = ManageWorkData(repository)

            val created = (data.saveProject("  Rediseño web 🎨  ", ProjectColor.PURPLE) as AppResult.Success).value
            data.saveProject("Rediseño", ProjectColor.TEAL, id = created.id)

            assertEquals(1, repository.current.projects.size)
            assertEquals(
                "Rediseño",
                repository.current.projects
                    .single()
                    .name,
            )
            assertEquals(WorkClockIssue.NAME_MISSING.name, reason(data.saveProject("   ", ProjectColor.BLUE)))
            assertEquals(WorkClockIssue.NAME_TOO_LONG.name, reason(data.saveProject("x".repeat(61), ProjectColor.BLUE)))
        }

    @Test
    fun `deleting a project keeps the shifts and undo restores it`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val data = ManageWorkData(repository)
            val project = (data.saveProject("Web", ProjectColor.BLUE) as AppResult.Success).value
            repository.saveShift(shift("a", "2026-03-12T09:00", "2026-03-12T11:00").copy(projectId = project.id))

            data.deleteProject(project.id)
            assertEquals(1, repository.current.shifts.size)
            data.restoreProject(project)

            assertEquals(listOf(project), repository.current.projects)
        }

    @Test
    fun `the project limit is enforced`() =
        runTest {
            val data = ManageWorkData(InMemoryWorkRepository())
            repeat(50) { data.saveProject("P$it", ProjectColor.BLUE) }

            assertEquals(WorkClockIssue.TOO_MANY_PROJECTS.name, reason(data.saveProject("una más", ProjectColor.BLUE)))
        }

    @Test
    fun `a blank note deletes the day note and a long one is rejected`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val data = ManageWorkData(repository)
            val date = LocalDate.of(2026, 3, 12)

            data.saveNote(date, "  Reunión con el cliente  ")
            assertEquals(
                "Reunión con el cliente",
                repository.current.notes
                    .single()
                    .text,
            )
            data.saveNote(date, "   ")
            assertTrue(repository.current.notes.isEmpty())

            assertEquals(WorkClockIssue.NOTE_TOO_LONG.name, reason(data.saveNote(date, "x".repeat(2_001))))
        }

    @Test
    fun `settings are validated before saving`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val data = ManageWorkData(repository)

            assertTrue(data.saveSettings(WorkSettings(weeklyTargetMinutes = 1_800)) is AppResult.Success)
            assertEquals(1_800, repository.current.settings.weeklyTargetMinutes)
            assertEquals(
                WorkClockIssue.TARGET_INVALID.name,
                reason(data.saveSettings(WorkSettings(weeklyTargetMinutes = 0))),
            )
            assertEquals(
                WorkClockIssue.NO_WORKING_DAYS.name,
                reason(data.saveSettings(WorkSettings(workingDays = emptySet()))),
            )
            assertEquals(1_800, repository.current.settings.weeklyTargetMinutes)
        }

    private object ShiftIssueNames {
        val endBeforeStart = com.example.lifet.core.model.work.ShiftIssue.END_BEFORE_START.name
        val endInFuture = com.example.lifet.core.model.work.ShiftIssue.END_IN_FUTURE.name
        val tooLong = com.example.lifet.core.model.work.ShiftIssue.TOO_LONG.name
        val breaksOverlap = com.example.lifet.core.model.work.ShiftIssue.BREAKS_OVERLAP.name
        val overlaps = com.example.lifet.core.model.work.ShiftIssue.OVERLAPS_OTHER_SHIFT.name
    }
}
