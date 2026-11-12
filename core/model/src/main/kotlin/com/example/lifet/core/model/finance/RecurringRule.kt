package com.example.lifet.core.model.finance

import com.example.lifet.core.model.Money
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

public enum class Frequency { WEEKLY, MONTHLY, YEARLY }

private const val DAYS_PER_WEEK = 7L

/**
 * Regla de movimiento recurrente (nómina, alquiler, suscripciones). [amount] lleva signo como un movimiento.
 * Las ocurrencias futuras se materializan como movimientos con fecha posterior a hoy (previstos).
 * [generatedThrough] es el último día para el que ya se generaron movimientos: borrar uno generado no lo resucita.
 */
public data class RecurringRule(
    val id: String,
    val name: String,
    val amount: Money,
    val categoryId: String,
    val accountId: String,
    val frequency: Frequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val active: Boolean = true,
    val generatedThrough: LocalDate? = null,
)

/**
 * Fechas de la regla en `[from, toInclusive]`. En mensual/anual el día se ajusta al último del mes
 * si el mes es más corto (día 31 en febrero, 29 de febrero en años no bisiestos).
 */
public fun RecurringRule.occurrences(
    from: LocalDate,
    toInclusive: LocalDate,
): List<LocalDate> {
    val last = endDate?.let { minOf(it, toInclusive) } ?: toInclusive
    val first = maxOf(from, startDate)
    if (!active || first > last) return emptyList()
    return when (frequency) {
        Frequency.WEEKLY -> weeklyDates(first, last)
        Frequency.MONTHLY -> monthlyDates(first, last)
        Frequency.YEARLY -> yearlyDates(first, last)
    }
}

private fun RecurringRule.weeklyDates(
    first: LocalDate,
    last: LocalDate,
): List<LocalDate> {
    val offset = Math.floorMod(ChronoUnit.DAYS.between(startDate, first), DAYS_PER_WEEK)
    val firstOccurrence = if (offset == 0L) first else first.plusDays(DAYS_PER_WEEK - offset)
    return generateSequence(firstOccurrence) { it.plusWeeks(1) }.takeWhile { it <= last }.toList()
}

private fun RecurringRule.monthlyDates(
    first: LocalDate,
    last: LocalDate,
): List<LocalDate> =
    generateSequence(YearMonth.from(first)) { it.plusMonths(1) }
        .takeWhile { it.atDay(1) <= last }
        .map { month -> month.atDay(minOf(startDate.dayOfMonth, month.lengthOfMonth())) }
        .filter { it >= first && it <= last }
        .toList()

private fun RecurringRule.yearlyDates(
    first: LocalDate,
    last: LocalDate,
): List<LocalDate> =
    (first.year..last.year)
        .map { year ->
            val month = YearMonth.of(year, startDate.month)
            month.atDay(minOf(startDate.dayOfMonth, month.lengthOfMonth()))
        }.filter { it >= first && it <= last }

/** Id estable de la ocurrencia: sembrar o sincronizar dos veces no duplica movimientos. */
public fun RecurringRule.occurrenceId(date: LocalDate): String = "rec_${id}_$date"

/**
 * Movimientos aún sin generar hasta [horizon] (incluido): desde el día siguiente a [generatedThrough]
 * o, si nunca se generó, desde [firstDay].
 */
public fun RecurringRule.pendingTransactions(
    firstDay: LocalDate,
    horizon: LocalDate,
    createdAt: Instant,
    zone: ZoneId,
): List<FinanceTransaction> {
    val from = generatedThrough?.plusDays(1) ?: firstDay
    return occurrences(from, horizon).map { date ->
        FinanceTransaction(
            id = occurrenceId(date),
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            date = date,
            note = name,
            recurringRuleId = id,
            createdAt = createdAt,
            zone = zone,
        )
    }
}
