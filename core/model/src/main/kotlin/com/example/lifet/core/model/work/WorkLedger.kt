package com.example.lifet.core.model.work

import java.time.Instant
import java.time.LocalDate

/**
 * Cuenta de horas por día de un [WorkDataSet] en el instante [now]. Es la base de todos los resúmenes.
 *
 * Reglas:
 * - Una jornada cuenta en el día en que empezó (una de noche que cruza la medianoche es una sola jornada).
 * - Se mide con instantes reales: pausas descontadas y cambio de hora (DST) sin desajustes.
 * - Una jornada abierta cuenta hasta [now] (cronómetro en vivo), salvo que esté olvidada: entonces no suma horas
 *   inventadas hasta que se cierre.
 * - Un día sin ninguna jornada contable no resta objetivo (se asume libre: vacaciones o festivo).
 * - El día en curso (o con una jornada aún abierta) nunca resta: su déficit sólo se materializa cuando termina,
 *   pero si ya se ha superado el objetivo del día el exceso sí suma.
 */
public class WorkLedger(
    private val data: WorkDataSet,
    private val now: Instant,
    public val today: LocalDate,
) {
    private val countedByDay: Map<LocalDate, List<WorkShift>> by lazy {
        data.shifts
            .filter { !it.isForgotten(now) && it.start <= now }
            .groupBy { it.startDate }
    }

    private val focusSecondsByDay: Map<LocalDate, Long> by lazy {
        data.focusSessions
            .filter { it.kind.isFocus && it.start <= now }
            .groupingBy { it.startDate }
            .fold(0L) { total, session -> total + session.elapsedSecondsAt(now) }
    }

    /** Jornadas contables de [date] (excluye las olvidadas), de la más antigua a la más reciente. */
    public fun shiftsOn(date: LocalDate): List<WorkShift> = countedByDay[date].orEmpty().sortedBy { it.start }

    /** Segundos trabajados en [date], pausas descontadas. */
    public fun workedSeconds(date: LocalDate): Long = countedByDay[date].orEmpty().sumOf { it.netSecondsAt(now) }

    public fun targetSeconds(date: LocalDate): Long = data.settings.targetSecondsOn(date)

    public fun focusSeconds(date: LocalDate): Long = focusSecondsByDay[date] ?: 0L

    /** Hay al menos una jornada contable ese día. */
    public fun hasRecord(date: LocalDate): Boolean = countedByDay.containsKey(date)

    /** Saldo del día: horas trabajadas menos objetivo, con las reglas de la documentación de la clase. */
    public fun balanceSeconds(date: LocalDate): Long {
        val shifts = countedByDay[date] ?: return 0L
        val difference = workedSeconds(date) - targetSeconds(date)
        val inProgress = date >= today || shifts.any { it.isOpen }
        return if (inProgress) difference.coerceAtLeast(0L) else difference
    }

    /** Saldo acumulado de todos los días hasta [until] (incluido). */
    public fun balanceThrough(until: LocalDate): Long =
        countedByDay.keys.filter { it <= until }.sumOf { balanceSeconds(it) }

    /** Saldo de los días de [from] a [until] (ambos incluidos). */
    public fun balanceBetween(
        from: LocalDate,
        until: LocalDate,
    ): Long = countedByDay.keys.filter { it in from..until }.sumOf { balanceSeconds(it) }

    public fun workedBetween(
        from: LocalDate,
        until: LocalDate,
    ): Long = countedByDay.keys.filter { it in from..until }.sumOf { workedSeconds(it) }

    /** Segundos trabajados por proyecto (clave `null` = sin proyecto o proyecto borrado) entre [from] y [until]. */
    public fun secondsByProject(
        from: LocalDate,
        until: LocalDate,
    ): Map<String?, Long> {
        val known = data.projects.mapTo(HashSet()) { it.id }
        val totals = LinkedHashMap<String?, Long>()
        countedByDay
            .filterKeys { it in from..until }
            .values
            .flatten()
            .forEach { shift ->
                val key = shift.projectId?.takeIf { it in known }
                totals.merge(key, shift.netSecondsAt(now), Long::plus)
            }
        return totals
    }

    public val firstDate: LocalDate? get() = countedByDay.keys.minOrNull()
}
