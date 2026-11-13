package com.example.lifet.core.model.work

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cálculos de Trabajo sobre un [WorkDataSet]. Pura y determinista: "ahora" y la zona llegan de fuera. "Hoy" es el día
 * de [now] en [zone]. Las reglas de horas (qué cuenta y en qué día) viven en [WorkLedger].
 */
public class WorkCalculator(
    private val data: WorkDataSet,
    private val now: Instant,
    private val zone: ZoneId,
) {
    private val today: LocalDate = now.atZone(zone).toLocalDate()
    private val ledger = WorkLedger(data, now, today)
    private val currentWeek = DateRange.containing(DateRangeUnit.WEEK, today)

    private val activeShift: WorkShift? by lazy {
        data.shifts.filter { it.isOpen && !it.isForgotten(now) && it.start <= now }.maxByOrNull { it.start }
    }

    private val forgottenShifts: List<WorkShift> by lazy {
        data.shifts.filter { it.isForgotten(now) }.sortedBy { it.start }
    }

    public fun todaySummary(): WorkTodaySummary {
        val active = activeShift
        return WorkTodaySummary(
            asOf = now,
            isClockedIn = active != null,
            shiftStart = active?.start,
            onBreak = active?.isOnBreak == true,
            needsClosing = forgottenShifts.isNotEmpty(),
            activeShiftSeconds = active?.netSecondsAt(now) ?: 0L,
            todaySeconds = ledger.workedSeconds(today),
            weekSeconds = ledger.workedBetween(currentWeek.start, currentWeek.endInclusive),
            weeklyTargetSeconds = data.settings.weeklyTargetSeconds,
            weekBalanceSeconds = ledger.balanceBetween(currentWeek.start, currentWeek.endInclusive),
            totalBalanceSeconds = ledger.balanceThrough(today),
            focusSecondsToday = ledger.focusSeconds(today),
            hasData = !data.hasNoRecords,
            liveToday = active?.startDate == today,
            liveWeek = active?.startDate?.let { it in currentWeek } == true,
        )
    }

    /** Un punto por día de [range] hasta hoy; los días sin jornada van con cero. */
    public fun series(range: DateRange): WorkSeries =
        WorkSeries(
            range,
            range.days.filter { it <= today }.map { date ->
                WorkDayPoint(
                    date = date,
                    workedSeconds = ledger.workedSeconds(date),
                    targetSeconds = ledger.targetSeconds(date),
                    balanceSeconds = ledger.balanceSeconds(date),
                    shifts = ledger.shiftsOn(date).size,
                    focusSeconds = ledger.focusSeconds(date),
                )
            },
        )

    /** Informe de la semana que contiene [week] (se alinea de lunes a domingo). */
    public fun weekReport(week: DateRange): WorkWeekReport {
        val range = DateRange.containing(DateRangeUnit.WEEK, week.start)
        val notes = data.notes.associate { it.date to it.text }
        val days =
            range.days.map { date ->
                WorkWeekDay(
                    date = date,
                    workedSeconds = ledger.workedSeconds(date),
                    targetSeconds = ledger.targetSeconds(date),
                    shifts = data.shifts.filter { it.startDate == date }.sortedBy { it.start },
                    note = notes[date].orEmpty(),
                    isFuture = date > today,
                )
            }
        return WorkWeekReport(
            week = range,
            days = days,
            workedSeconds = ledger.workedBetween(range.start, range.endInclusive),
            weeklyTargetSeconds = data.settings.weeklyTargetSeconds,
            balanceSeconds = ledger.balanceBetween(range.start, range.endInclusive),
            projectShares = shares(range.start, range.endInclusive),
            balances = weekBalances(range),
            isCurrentWeek = today in range,
            settings = data.settings,
        )
    }

    public fun overview(): WorkOverview {
        val recentFrom = today.minusDays(RECENT_PROJECT_DAYS - 1L)
        return WorkOverview(
            now = now,
            today = today,
            summary = todaySummary(),
            activeShift = activeShift,
            forgottenShifts = forgottenShifts,
            todayShifts = data.shifts.filter { it.startDate == today }.sortedByDescending { it.start },
            todayNote =
                data.notes
                    .firstOrNull { it.date == today }
                    ?.text
                    .orEmpty(),
            focus = focusOverview(),
            projects = projectTotals(recentFrom),
            projectShares = shares(recentFrom, today),
            settings = data.settings,
            hasAnyRecord = !data.hasNoRecords,
        )
    }

    private fun focusOverview(): FocusOverview {
        val running = data.focusSessions.filter { it.isRunning }.maxByOrNull { it.start }
        val todaySessions = data.focusSessions.filter { it.startDate == today }.sortedByDescending { it.start }
        val completedToday = todaySessions.count { it.kind.isFocus && it.completed }
        val lastFinished = todaySessions.firstOrNull { !it.isRunning }
        return FocusOverview(
            running = running,
            todaySessions = todaySessions,
            todayFocusSeconds = ledger.focusSeconds(today),
            completedFocusToday = completedToday,
            suggestedNext = suggestedNext(lastFinished, completedToday),
            recent =
                data.focusSessions
                    .filter { it.kind.isFocus && !it.isRunning && it.completed }
                    .sortedByDescending { it.start }
                    .take(WorkRules.RECENT_FOCUS_SESSIONS),
        )
    }

    /** Tras un foco completo toca descansar (largo cada cuatro); tras un descanso o un foco cancelado, otro foco. */
    private fun suggestedNext(
        lastFinished: FocusSession?,
        completedToday: Int,
    ): FocusKind =
        when {
            lastFinished == null || !lastFinished.kind.isFocus || !lastFinished.completed -> {
                FocusKind.FOCUS
            }

            completedToday % WorkRules.FOCUS_ROUNDS_BEFORE_LONG_BREAK == 0 -> {
                FocusKind.LONG_BREAK
            }

            else -> {
                FocusKind.SHORT_BREAK
            }
        }

    private fun projectTotals(recentFrom: LocalDate): List<ProjectTotals> {
        val allTime = ledger.secondsByProject(LocalDate.MIN, LocalDate.MAX)
        val recent = ledger.secondsByProject(recentFrom, today)
        val thisWeek = ledger.secondsByProject(currentWeek.start, currentWeek.endInclusive)
        val focusByProject =
            data.focusSessions
                .filter { it.kind.isFocus && it.projectId != null }
                .groupingBy { it.projectId }
                .fold(0L) { total, session -> total + session.elapsedSecondsAt(now) }
        return data.projects
            .map { project ->
                ProjectTotals(
                    project = project,
                    weekSeconds = thisWeek[project.id] ?: 0L,
                    recentSeconds = recent[project.id] ?: 0L,
                    totalSeconds = allTime[project.id] ?: 0L,
                    focusSeconds = focusByProject[project.id] ?: 0L,
                )
            }.sortedWith(compareByDescending<ProjectTotals> { it.recentSeconds }.thenBy { it.project.name.lowercase() })
    }

    private fun shares(
        from: LocalDate,
        until: LocalDate,
    ): List<ProjectShare> {
        val byId = data.projects.associateBy { it.id }
        return ledger
            .secondsByProject(from, until)
            .filterValues { it > 0L }
            .map { (id, seconds) -> ProjectShare(id?.let(byId::get), seconds) }
            .sortedByDescending { it.seconds }
    }

    /** Las últimas semanas hasta [range], con el saldo de cada una y el acumulado hasta su domingo. */
    private fun weekBalances(range: DateRange): List<WeekBalance> =
        (WorkRules.CHART_WEEKS - 1 downTo 0).map { back ->
            val week = range.shift(-back.toLong())
            WeekBalance(
                weekStart = week.start,
                balanceSeconds = ledger.balanceBetween(week.start, week.endInclusive),
                cumulativeSeconds = ledger.balanceThrough(week.endInclusive),
            )
        }

    private companion object {
        const val RECENT_PROJECT_DAYS = 28
    }
}
