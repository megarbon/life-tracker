package com.example.lifet.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Granularidad de un [DateRange]. */
public enum class DateRangeUnit { DAY, WEEK, MONTH, YEAR }

/**
 * Rango de fechas semiabierto `[start, endExclusive)` alineado a la unidad (semana de lunes a domingo).
 * Es la entrada común de `Observe*Series` y del selector de rango de la UI.
 */
public data class DateRange(
    val unit: DateRangeUnit,
    val start: LocalDate,
) {
    val endExclusive: LocalDate
        get() =
            when (unit) {
                DateRangeUnit.DAY -> start.plusDays(1)
                DateRangeUnit.WEEK -> start.plusWeeks(1)
                DateRangeUnit.MONTH -> start.plusMonths(1)
                DateRangeUnit.YEAR -> start.plusYears(1)
            }

    /** Último día incluido. */
    val endInclusive: LocalDate get() = endExclusive.minusDays(1)

    val days: List<LocalDate>
        get() = generateSequence(start) { it.plusDays(1) }.takeWhile { it < endExclusive }.toList()

    public operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive

    /** Desplaza el rango [steps] unidades (negativo = pasado). */
    public fun shift(steps: Long): DateRange =
        copy(
            start =
                when (unit) {
                    DateRangeUnit.DAY -> start.plusDays(steps)
                    DateRangeUnit.WEEK -> start.plusWeeks(steps)
                    DateRangeUnit.MONTH -> start.plusMonths(steps)
                    DateRangeUnit.YEAR -> start.plusYears(steps)
                },
        )

    public companion object {
        /** Rango de [unit] que contiene a [date]. */
        public fun containing(
            unit: DateRangeUnit,
            date: LocalDate,
        ): DateRange =
            DateRange(
                unit,
                when (unit) {
                    DateRangeUnit.DAY -> date
                    DateRangeUnit.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    DateRangeUnit.MONTH -> date.withDayOfMonth(1)
                    DateRangeUnit.YEAR -> date.withDayOfYear(1)
                },
            )
    }
}
