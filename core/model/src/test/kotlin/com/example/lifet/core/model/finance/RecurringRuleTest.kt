package com.example.lifet.core.model.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecurringRuleTest {
    private val rule =
        RecurringRule(
            id = "rent",
            name = "Alquiler",
            amount = Money(-80_000, CurrencyCode.EUR),
            categoryId = DefaultCategory.HOUSING.id,
            accountId = "bank",
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 31),
        )

    @Test
    fun `monthly on day 31 falls back to the last day of shorter months`() {
        val dates = rule.occurrences(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30))

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            dates,
        )
    }

    @Test
    fun `weekly keeps the weekday of the start date`() {
        val weekly = rule.copy(frequency = Frequency.WEEKLY, startDate = LocalDate.of(2026, 3, 2))

        val dates = weekly.occurrences(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 20))

        assertEquals(listOf(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 16)), dates)
    }

    @Test
    fun `yearly on 29 February uses 28 February in non leap years`() {
        val yearly = rule.copy(frequency = Frequency.YEARLY, startDate = LocalDate.of(2024, 2, 29))

        val dates = yearly.occurrences(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31))

        assertEquals(
            listOf(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28)),
            dates,
        )
    }

    @Test
    fun `inactive or ended rules produce nothing after the end`() {
        assertTrue(rule.copy(active = false).occurrences(LocalDate.MIN, LocalDate.of(2026, 12, 31)).isEmpty())
        val ended = rule.copy(endDate = LocalDate.of(2026, 2, 15))
        assertEquals(listOf(LocalDate.of(2026, 1, 31)), ended.occurrences(LocalDate.MIN, LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun `pending transactions start after generatedThrough and have stable ids`() {
        val generated = rule.copy(generatedThrough = LocalDate.of(2026, 2, 28))

        val pending =
            generated.pendingTransactions(
                firstDay = LocalDate.of(2026, 1, 1),
                horizon = LocalDate.of(2026, 3, 31),
                createdAt = Instant.EPOCH,
                zone = ZoneId.of("Europe/Madrid"),
            )

        assertEquals(1, pending.size)
        assertEquals("rec_rent_2026-03-31", pending.single().id)
        assertEquals("rent", pending.single().recurringRuleId)
        assertEquals(Money(-80_000, CurrencyCode.EUR), pending.single().amount)
    }
}
