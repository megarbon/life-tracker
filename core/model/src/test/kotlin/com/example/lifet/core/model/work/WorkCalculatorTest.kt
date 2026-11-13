package com.example.lifet.core.model.work

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WorkCalculatorTest {
    // Jueves 12 de marzo de 2026, 12:00 en Madrid.
    private val now = at("2026-03-12T12:00")
    private val settings = WorkSettings(weeklyTargetMinutes = 40 * 60)

    private fun calculator(
        shifts: List<WorkShift> = emptyList(),
        projects: List<WorkProject> = emptyList(),
        focus: List<FocusSession> = emptyList(),
        notes: List<WorkDayNote> = emptyList(),
        at: java.time.Instant = now,
    ) = WorkCalculator(WorkDataSet(shifts, projects, focus, notes, settings), at, MADRID)

    private fun eightHours(
        id: String,
        date: String,
    ) = shift(id, "${date}T09:00", "${date}T17:00")

    @Test
    fun `an unused area has no data and a zero summary`() {
        val summary = calculator().todaySummary()

        assertFalse(summary.hasData)
        assertFalse(summary.isClockedIn)
        assertEquals(0L, summary.weekSeconds)
        assertEquals(40 * HOUR, summary.weeklyTargetSeconds)
        assertEquals(0L, summary.totalBalanceSeconds)
    }

    @Test
    fun `an open shift is clocked in and the stopwatch keeps growing without a new query`() {
        val summary = calculator(listOf(shift("a", "2026-03-12T09:00", null))).todaySummary()

        assertTrue(summary.isClockedIn)
        assertEquals(at("2026-03-12T09:00"), summary.shiftStart)
        assertEquals(3 * HOUR, summary.activeShiftSeconds)
        assertEquals(3 * HOUR, summary.todaySeconds)
        assertEquals(3 * HOUR + 60, summary.shiftSecondsAt(now.plusSeconds(60)))
        assertEquals(3 * HOUR + 60, summary.todaySecondsAt(now.plusSeconds(60)))
        assertEquals(3 * HOUR + 60, summary.weekSecondsAt(now.plusSeconds(60)))
    }

    @Test
    fun `the stopwatch does not advance while on a break`() {
        val onBreak = shift("a", "2026-03-12T09:00", null, breaks = listOf(pause("2026-03-12T11:00", null)))

        val summary = calculator(listOf(onBreak)).todaySummary()

        assertTrue(summary.onBreak)
        assertEquals(2 * HOUR, summary.shiftSecondsAt(now.plusSeconds(600)))
    }

    @Test
    fun `an open shift from the day before is forgotten, not counted and flagged for closing`() {
        val forgotten = shift("a", "2026-03-10T09:00", null)

        val calculator = calculator(listOf(forgotten, eightHours("b", "2026-03-11")))
        val summary = calculator.todaySummary()

        assertFalse(summary.isClockedIn)
        assertTrue(summary.needsClosing)
        assertEquals(8 * HOUR, summary.weekSeconds)
        assertEquals(listOf("a"), calculator.overview().forgottenShifts.map { it.id })
    }

    @Test
    fun `closing a forgotten shift afterwards counts its hours`() {
        val closed = shift("a", "2026-03-10T09:00", "2026-03-10T18:00")

        val summary = calculator(listOf(closed)).todaySummary()

        assertFalse(summary.needsClosing)
        assertEquals(9 * HOUR, summary.weekSeconds)
    }

    @Test
    fun `week balance counts overtime of finished days and ignores free days`() {
        // Lunes 9h, martes 8h, miércoles sin jornada (libre): no resta; el jueves en curso no resta.
        val shifts =
            listOf(
                shift("a", "2026-03-09T09:00", "2026-03-09T18:00"),
                eightHours("b", "2026-03-10"),
            )

        val summary = calculator(shifts).todaySummary()

        assertEquals(17 * HOUR, summary.weekSeconds)
        assertEquals(1 * HOUR, summary.weekBalanceSeconds)
    }

    @Test
    fun `a short finished day subtracts from the balance`() {
        val summary = calculator(listOf(shift("a", "2026-03-10T09:00", "2026-03-10T14:00"))).todaySummary()

        assertEquals(-3 * HOUR, summary.weekBalanceSeconds)
    }

    @Test
    fun `today never subtracts while it is in progress but overtime already counts`() {
        val short = calculator(listOf(shift("a", "2026-03-12T09:00", null))).todaySummary()
        val long = calculator(listOf(shift("a", "2026-03-12T01:00", null)), at = at("2026-03-12T12:00")).todaySummary()

        assertEquals(0L, short.weekBalanceSeconds)
        assertEquals(3 * HOUR, long.weekBalanceSeconds)
    }

    @Test
    fun `weekend work is pure overtime because those days have no target`() {
        val summary = calculator(listOf(shift("a", "2026-03-07T10:00", "2026-03-07T14:00"))).todaySummary()

        assertEquals(4 * HOUR, summary.totalBalanceSeconds)
        assertEquals(0L, summary.weekBalanceSeconds)
    }

    @Test
    fun `total balance accumulates across weeks`() {
        val shifts =
            listOf(
                shift("a", "2026-03-02T09:00", "2026-03-02T19:00"),
                shift("b", "2026-03-03T09:00", "2026-03-03T16:00"),
                shift("c", "2026-03-09T09:00", "2026-03-09T18:00"),
            )

        assertEquals((2 - 1 + 1) * HOUR, calculator(shifts).todaySummary().totalBalanceSeconds)
    }

    @Test
    fun `a night shift is counted once on the day it started`() {
        val night = shift("a", "2026-03-10T22:00", "2026-03-11T06:00")
        val series = calculator(listOf(night)).series(DateRange.containing(DateRangeUnit.WEEK, day("2026-03-12")))

        assertEquals(8 * HOUR, series.days.first { it.date == day("2026-03-10") }.workedSeconds)
        assertEquals(0L, series.days.first { it.date == day("2026-03-11") }.workedSeconds)
    }

    @Test
    fun `the series covers every day up to today and fills empty days with zero`() {
        val week = DateRange.containing(DateRangeUnit.WEEK, day("2026-03-12"))

        val series = calculator(listOf(eightHours("a", "2026-03-10"))).series(week)

        assertEquals(4, series.days.size)
        assertEquals(listOf(0L, 8 * HOUR, 0L, 0L), series.days.map { it.workedSeconds })
        assertEquals(8 * HOUR, series.days[1].targetSeconds)
        assertEquals(
            0L,
            calculator()
                .series(week.shift(1))
                .days.size
                .toLong(),
        )
    }

    @Test
    fun `a day with a target but no shift has zero balance`() {
        val series =
            calculator(
                listOf(eightHours("a", "2026-03-10")),
            ).series(DateRange(DateRangeUnit.WEEK, day("2026-03-09")))

        assertEquals(0L, series.days.first { it.date == day("2026-03-11") }.balanceSeconds)
    }

    @Test
    fun `week report lists seven days, flags the future and carries the notes`() {
        val report =
            calculator(
                shifts = listOf(eightHours("a", "2026-03-10")),
                notes = listOf(WorkDayNote(day("2026-03-10"), "Reunión larga")),
            ).weekReport(DateRange.containing(DateRangeUnit.WEEK, day("2026-03-12")))

        assertEquals(7, report.days.size)
        assertEquals("Reunión larga", report.days[1].note)
        assertEquals(listOf(false, false, false, false, true, true, true), report.days.map { it.isFuture })
        assertTrue(report.isCurrentWeek)
        assertEquals(8 * HOUR, report.workedSeconds)
    }

    @Test
    fun `the week report cumulative balance ends at the total balance of its sunday`() {
        val shifts =
            listOf(
                shift("a", "2026-03-02T09:00", "2026-03-02T19:00"),
                shift("b", "2026-03-09T09:00", "2026-03-09T18:00"),
            )

        val report = calculator(shifts).weekReport(DateRange.containing(DateRangeUnit.WEEK, day("2026-03-12")))

        assertEquals(WorkRules.CHART_WEEKS, report.balances.size)
        assertEquals(3 * HOUR, report.balances.last().cumulativeSeconds)
        assertEquals(1 * HOUR, report.balances.last().balanceSeconds)
        assertEquals(2 * HOUR, report.balances[report.balances.size - 2].balanceSeconds)
        assertEquals(2 * HOUR, report.balances[report.balances.size - 2].cumulativeSeconds)
    }

    @Test
    fun `hours are split by project and an unknown project falls under none`() {
        val projects = listOf(WorkProject("p1", "Web", ProjectColor.BLUE))
        val shifts =
            listOf(
                shift("a", "2026-03-10T09:00", "2026-03-10T13:00", projectId = "p1"),
                shift("b", "2026-03-10T14:00", "2026-03-10T16:00", projectId = "borrado"),
                shift("c", "2026-03-11T09:00", "2026-03-11T10:00"),
            )

        val shares =
            calculator(
                shifts,
                projects,
            ).weekReport(DateRange.containing(DateRangeUnit.WEEK, day("2026-03-12"))).projectShares

        assertEquals("Web", shares[0].project?.name)
        assertEquals(4 * HOUR, shares[0].seconds)
        assertNull(shares[1].project)
        assertEquals(3 * HOUR, shares[1].seconds)
    }

    @Test
    fun `project totals sort by recent hours and include focus time`() {
        val projects = listOf(WorkProject("p1", "Web", ProjectColor.BLUE), WorkProject("p2", "App", ProjectColor.TEAL))
        val shifts = listOf(shift("a", "2026-03-10T09:00", "2026-03-10T11:00", projectId = "p2"))
        val focus =
            listOf(
                FocusSession(
                    "f1",
                    FocusKind.FOCUS,
                    at("2026-03-10T09:00"),
                    MADRID,
                    1_500,
                    end = at("2026-03-10T09:25"),
                    completed = true,
                    projectId = "p2",
                ),
            )

        val totals = calculator(shifts, projects, focus).overview().projects

        assertEquals(listOf("App", "Web"), totals.map { it.project.name })
        assertEquals(2 * HOUR, totals[0].totalSeconds)
        assertEquals(1_500L, totals[0].focusSeconds)
        assertEquals(0L, totals[1].totalSeconds)
    }

    @Test
    fun `settings split the weekly target over the working days`() {
        val custom =
            WorkSettings(weeklyTargetMinutes = 20 * 60, workingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))

        assertEquals(10 * HOUR, custom.targetSecondsOn(day("2026-03-09")))
        assertEquals(0L, custom.targetSecondsOn(day("2026-03-11")))
        assertTrue(custom.isValid)
        assertFalse(WorkSettings(weeklyTargetMinutes = 0).isValid)
        assertFalse(WorkSettings(workingDays = emptySet()).isValid)
        assertEquals(0L, WorkSettings(workingDays = emptySet()).targetSecondsOn(day("2026-03-09")))
    }

    @Test
    fun `week progress is clamped and reaching the target is flagged`() {
        val shifts = (9..13).map { shift("s$it", "2026-03-%02dT09:00".format(it), "2026-03-%02dT18:00".format(it)) }

        val summary =
            WorkCalculator(
                WorkDataSet(shifts, settings = settings),
                at("2026-03-14T12:00"),
                MADRID,
            ).todaySummary()

        assertEquals(1f, summary.weekProgress, 0f)
        assertTrue(summary.weekTargetReached)
    }

    @Test
    fun `focus time counts only focus intervals of the day`() {
        val focus =
            listOf(
                FocusSession(
                    "f1",
                    FocusKind.FOCUS,
                    at("2026-03-12T09:00"),
                    MADRID,
                    1_500,
                    at("2026-03-12T09:25"),
                    true,
                ),
                FocusSession(
                    "f2",
                    FocusKind.SHORT_BREAK,
                    at("2026-03-12T09:25"),
                    MADRID,
                    300,
                    at("2026-03-12T09:30"),
                    true,
                ),
                FocusSession(
                    "f3",
                    FocusKind.FOCUS,
                    at("2026-03-11T09:00"),
                    MADRID,
                    1_500,
                    at("2026-03-11T09:25"),
                    true,
                ),
            )

        val summary = calculator(focus = focus).todaySummary()

        assertEquals(1_500L, summary.focusSecondsToday)
        assertTrue(summary.hasData)
    }

    @Test
    fun `after a completed focus a break is suggested and every fourth is long`() {
        fun done(
            index: Int,
            kind: FocusKind = FocusKind.FOCUS,
            completed: Boolean = true,
        ): FocusSession {
            val start = at("2026-03-12T08:00").plusSeconds(index * 1_800L)
            return FocusSession("f$index", kind, start, MADRID, 1_500, start.plusSeconds(1_500), completed)
        }

        assertEquals(FocusKind.FOCUS, calculator().overview().focus.suggestedNext)
        assertEquals(FocusKind.SHORT_BREAK, calculator(focus = listOf(done(0))).overview().focus.suggestedNext)
        assertEquals(
            FocusKind.LONG_BREAK,
            calculator(focus = (0..3).map { done(it) }).overview().focus.suggestedNext,
        )
        assertEquals(
            FocusKind.FOCUS,
            calculator(focus = listOf(done(0), done(1, FocusKind.SHORT_BREAK))).overview().focus.suggestedNext,
        )
        assertEquals(
            FocusKind.FOCUS,
            calculator(focus = listOf(done(0, completed = false))).overview().focus.suggestedNext,
        )
    }

    @Test
    fun `a shift in progress when the clock crosses the DST change still measures real time`() {
        val summary =
            WorkCalculator(
                WorkDataSet(listOf(shift("a", "2026-03-28T22:00", null)), settings = settings),
                at("2026-03-29T06:00"),
                MADRID,
            ).todaySummary()

        assertEquals(7 * HOUR, summary.activeShiftSeconds)
    }

    @Test
    fun `a night shift still running from yesterday is listed among todays shifts`() {
        // Entrada ayer a las 22:00 y sigue abierta a las 02:00: no puede desaparecer de la lista de hoy.
        val night = shift("a", "2026-03-11T22:00", null)
        val overview = calculator(listOf(night), at = at("2026-03-12T02:00")).overview()

        assertEquals(listOf("a"), overview.todayShifts.map { it.id })
        assertTrue(overview.summary.isClockedIn)
    }

    @Test
    fun `an active shift is not duplicated when it also started today`() {
        val overview = calculator(listOf(shift("a", "2026-03-12T09:00", null))).overview()

        assertEquals(1, overview.todayShifts.size)
    }

    @Test
    fun `live totals keep growing from the emission instant while clocked in`() {
        val summary = calculator(listOf(shift("a", "2026-03-12T09:00", null))).todaySummary()
        val later = now.plusSeconds(600)

        assertEquals(summary.todaySeconds + 600, summary.todaySecondsAt(later))
        assertEquals(summary.weekSeconds + 600, summary.weekSecondsAt(later))
        assertEquals(summary.activeShiftSeconds + 600, summary.shiftSecondsAt(later))
    }

    @Test
    fun `the live week balance stays at zero until the running day passes its target`() {
        // 3 h trabajadas de un objetivo de 8: el día en curso no resta ni suma hasta superar las 8 h.
        val summary = calculator(listOf(shift("a", "2026-03-12T09:00", null))).todaySummary()

        assertEquals(0L, summary.weekBalanceSecondsAt(now))
        assertEquals(0L, summary.weekBalanceSecondsAt(now.plusSeconds(4 * HOUR)))
        assertEquals(HOUR, summary.weekBalanceSecondsAt(now.plusSeconds(6 * HOUR)))
    }

    @Test
    fun `the live week balance matches what the ledger computes at that later instant`() {
        val data = listOf(shift("a", "2026-03-12T01:00", null))
        val earlier = calculator(data, at = at("2026-03-12T12:00")).todaySummary()
        val later = calculator(data, at = at("2026-03-12T15:00")).todaySummary()

        assertEquals(later.weekBalanceSeconds, earlier.weekBalanceSecondsAt(at("2026-03-12T15:00")))
    }

    @Test
    fun `no live time is added while on a break or with nothing running`() {
        val onBreak = shift("a", "2026-03-12T09:00", null, breaks = listOf(pause("2026-03-12T11:00", null)))
        val paused = calculator(listOf(onBreak)).todaySummary()
        val idle = calculator(listOf(shift("b", "2026-03-11T09:00", "2026-03-11T17:00"))).todaySummary()

        assertEquals(paused.todaySeconds, paused.todaySecondsAt(now.plusSeconds(3_600)))
        assertEquals(idle.weekSeconds, idle.weekSecondsAt(now.plusSeconds(3_600)))
    }
}
