package com.example.lifet.core.model.work

import com.example.lifet.core.model.DateRange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Lo que la pantalla Hoy necesita de Trabajo.
 *
 * Las cifras son las de [asOf]. Con una jornada en marcha (y sin pausa) siguen creciendo: [shiftSecondsAt],
 * [todaySecondsAt] y [weekSecondsAt] suman lo transcurrido desde [asOf], de modo que un cronómetro vivo no necesita
 * volver a consultar la base de datos cada segundo.
 *
 * @property isClockedIn hay una jornada en curso (abierta y no olvidada); [shiftStart] es su entrada.
 * @property onBreak la jornada en curso está en pausa (el cronómetro no avanza).
 * @property needsClosing hay jornadas abiertas olvidadas pendientes de cerrar a posteriori.
 * @property activeShiftSeconds tiempo trabajado de la jornada en curso (pausas descontadas).
 * @property weekBalanceSeconds saldo de la semana en curso (horas extra o a deber), con las reglas de [WorkLedger].
 * @property totalBalanceSeconds saldo acumulado de todo el histórico.
 * @property hasData `false` si el área aún no se ha usado: Hoy debe invitar a fichar la primera entrada.
 * @property liveToday y [liveWeek] indican si la jornada en curso cuenta en las cifras de hoy y de la semana.
 * @property liveDaySurplusSeconds lo trabajado menos el objetivo en el día de la jornada en curso, en [asOf].
 */
public data class WorkTodaySummary(
    public val asOf: Instant,
    public val isClockedIn: Boolean,
    public val shiftStart: Instant?,
    public val onBreak: Boolean,
    public val needsClosing: Boolean,
    public val activeShiftSeconds: Long,
    public val todaySeconds: Long,
    public val weekSeconds: Long,
    public val weeklyTargetSeconds: Long,
    public val weekBalanceSeconds: Long,
    public val totalBalanceSeconds: Long,
    public val focusSecondsToday: Long,
    public val hasData: Boolean,
    public val liveToday: Boolean,
    public val liveDaySurplusSeconds: Long,
    public val liveWeek: Boolean,
) {
    /** Progreso de la semana hacia el objetivo, entre 0 y 1. */
    public val weekProgress: Float
        get() =
            if (weeklyTargetSeconds <= 0L) 0f else (weekSeconds.toFloat() / weeklyTargetSeconds).coerceIn(0f, 1f)

    public val weekTargetReached: Boolean get() = weeklyTargetSeconds > 0L && weekSeconds >= weeklyTargetSeconds

    /** Segundos que han pasado desde [asOf] y cuentan como trabajados (cero si no hay jornada en marcha). */
    private fun liveDelta(now: Instant): Long =
        if (!isClockedIn || onBreak || now <= asOf) 0L else Duration.between(asOf, now).seconds

    public fun shiftSecondsAt(now: Instant): Long = activeShiftSeconds + liveDelta(now)

    public fun todaySecondsAt(now: Instant): Long = todaySeconds + if (liveToday) liveDelta(now) else 0L

    public fun weekSecondsAt(now: Instant): Long = weekSeconds + if (liveWeek) liveDelta(now) else 0L

    /** Saldo de la semana en [now]: el día en curso no resta, así que sólo suma una vez superado su objetivo. */
    public fun weekBalanceSecondsAt(now: Instant): Long {
        if (!liveWeek) return weekBalanceSeconds
        // El día en curso nunca resta: su contribución al saldo es max(0, trabajado - objetivo).
        val before = liveDaySurplusSeconds.coerceAtLeast(0L)
        val after = (liveDaySurplusSeconds + liveDelta(now)).coerceAtLeast(0L)
        return weekBalanceSeconds + after - before
    }
}

/** Un día de la serie. Los días sin jornada llevan cero (no se omiten). */
public data class WorkDayPoint(
    public val date: LocalDate,
    public val workedSeconds: Long,
    public val targetSeconds: Long,
    public val balanceSeconds: Long,
    public val shifts: Int,
    public val focusSeconds: Long,
)

/** Serie diaria de un [range], pensada para gráficas e Insights. Sólo incluye días hasta hoy. */
public data class WorkSeries(
    public val range: DateRange,
    public val days: List<WorkDayPoint>,
) {
    public val workedSeconds: Long get() = days.sumOf { it.workedSeconds }

    public val targetSeconds: Long get() = days.sumOf { it.targetSeconds }

    public val balanceSeconds: Long get() = days.sumOf { it.balanceSeconds }

    public val workedDays: Int get() = days.count { it.workedSeconds > 0L }
}

/** Reparto de horas de un proyecto ([project] `null` = sin proyecto o proyecto borrado). */
public data class ProjectShare(
    public val project: WorkProject?,
    public val seconds: Long,
)

/** Un día de la semana en pantalla. [shifts] incluye las jornadas abiertas y las olvidadas de ese día. */
public data class WorkWeekDay(
    public val date: LocalDate,
    public val workedSeconds: Long,
    public val targetSeconds: Long,
    public val shifts: List<WorkShift>,
    public val note: String,
    public val isFuture: Boolean,
)

/** Saldo de una semana y acumulado hasta su final, para la gráfica de saldo de horas extra. */
public data class WeekBalance(
    public val weekStart: LocalDate,
    public val balanceSeconds: Long,
    public val cumulativeSeconds: Long,
)

/**
 * La semana [week] de lunes a domingo: horas por día frente al objetivo, saldo, reparto por proyecto y las últimas
 * semanas de saldo acumulado que terminan en ella.
 */
public data class WorkWeekReport(
    public val week: DateRange,
    public val days: List<WorkWeekDay>,
    public val workedSeconds: Long,
    public val weeklyTargetSeconds: Long,
    public val balanceSeconds: Long,
    public val projectShares: List<ProjectShare>,
    public val balances: List<WeekBalance>,
    public val isCurrentWeek: Boolean,
    public val settings: WorkSettings,
)

/** Estado del temporizador de foco y de los pomodoros de hoy. */
public data class FocusOverview(
    public val running: FocusSession?,
    public val todaySessions: List<FocusSession>,
    public val todayFocusSeconds: Long,
    public val completedFocusToday: Int,
    public val suggestedNext: FocusKind,
    public val recent: List<FocusSession>,
)

/** Horas de un proyecto: esta semana, en los últimos 28 días, en total y de foco. */
public data class ProjectTotals(
    public val project: WorkProject,
    public val weekSeconds: Long,
    public val recentSeconds: Long,
    public val totalSeconds: Long,
    public val focusSeconds: Long,
)

/**
 * Todo lo que necesitan las pestañas Hoy, Foco y Proyectos, recalculado con cada cambio en la base de datos.
 *
 * @property activeShift jornada en curso (abierta y no olvidada).
 * @property forgottenShifts jornadas abiertas desde hace demasiado: se cierran a posteriori indicando la salida.
 * @property todayShifts jornadas que empezaron hoy, la más reciente primero (incluida la abierta).
 * @property projectShares reparto de horas de los últimos 28 días, de más a menos.
 */
public data class WorkOverview(
    public val now: Instant,
    public val today: LocalDate,
    public val summary: WorkTodaySummary,
    public val activeShift: WorkShift?,
    public val forgottenShifts: List<WorkShift>,
    public val todayShifts: List<WorkShift>,
    public val todayNote: String,
    public val focus: FocusOverview,
    public val projects: List<ProjectTotals>,
    public val projectShares: List<ProjectShare>,
    public val settings: WorkSettings,
    public val hasAnyRecord: Boolean,
)
