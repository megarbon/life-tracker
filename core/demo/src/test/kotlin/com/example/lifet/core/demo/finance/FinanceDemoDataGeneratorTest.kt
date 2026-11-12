package com.example.lifet.core.demo.finance

import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.primaryCurrency
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.finance.InMemoryFinanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class FinanceDemoDataGeneratorTest {
    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val today = LocalDate.of(2026, 3, 10)

    private suspend fun seeded(scenario: DemoScenario): InMemoryFinanceRepository {
        val repository = InMemoryFinanceRepository()
        FinanceDemoDataGenerator(repository).seed(scenario, DemoContext(clock))
        return repository
    }

    @Test
    fun `the same seed and clock give exactly the same data`() =
        runTest {
            assertEquals(seeded(DemoScenario.REGULAR).current, seeded(DemoScenario.REGULAR).current)
        }

    @Test
    fun `empty keeps categories and the main account but no movements`() =
        runTest {
            val data = seeded(DemoScenario.EMPTY).current

            assertTrue(data.transactions.isEmpty())
            assertEquals(DefaultCategory.entries.size, data.categories.size)
            assertEquals(1, data.accounts.size)
        }

    @Test
    fun `new user has about three days of data`() =
        runTest {
            val dates = seeded(DemoScenario.NEW_USER).current.transactions.map { it.date }.toSet()

            assertEquals(setOf(today.minusDays(2), today.minusDays(1), today), dates)
        }

    @Test
    fun `regular has six months with payroll on day 1, rent, subscriptions and weekly groceries`() =
        runTest {
            val data = seeded(DemoScenario.REGULAR).current
            val txs = data.transactions

            val salaries = txs.filter { it.categoryId == DefaultCategory.SALARY.id && it.date <= today }
            assertEquals(7, salaries.size)
            assertTrue(salaries.all { it.date.dayOfMonth == 1 })
            assertEquals(3, txs.filter { it.categoryId == DefaultCategory.SUBSCRIPTIONS.id }.map { it.note }.toSet().size)
            assertTrue(txs.any { it.categoryId == DefaultCategory.HOUSING.id })
            val groceriesPerWeek =
                txs
                    .filter { it.categoryId == DefaultCategory.GROCERIES.id && it.date < today.minusDays(7) }
                    .groupBy { it.date.with(DayOfWeek.MONDAY) }
                    .toSortedMap()
                    .values
                    .drop(1) // la primera semana del histórico es parcial
                    .dropLast(1) // y la última se corta por el filtro de fecha
                    .map { it.size }
            assertTrue(groceriesPerWeek.all { it in 2..4 })
            val leisure = txs.filter { it.categoryId == DefaultCategory.LEISURE.id && it.note != "Concierto 🎸" }
            assertTrue(leisure.filter { it.amount.amountMinor < 0 }.all { it.date.dayOfWeek.value >= 6 || it.note == "Escape room" })
            assertTrue(txs.any { it.isPlannedAt(today) })
        }

    @Test
    fun `regular has one month over budget and a refund`() =
        runTest {
            val data = seeded(DemoScenario.REGULAR).current
            val calculator = FinanceCalculator(data, data.primaryCurrency(), today)

            val overMonths =
                (1L..6L)
                    .map { YearMonth.from(today).minusMonths(it) }
                    .filter { month -> calculator.monthSummary(month).byCategory.any { it.isOver } }
            assertTrue(YearMonth.from(today).minusMonths(3) in overMonths)
            assertTrue(data.transactions.any { it.categoryId == DefaultCategory.SHOPPING.id && !it.amount.isNegative })
        }

    @Test
    fun `edge cases include other currencies, huge amounts and long texts`() =
        runTest {
            val data = seeded(DemoScenario.EDGE_CASES).current
            val currencies = data.transactions.map { it.amount.currency }.toSet()

            assertTrue(CurrencyCode("USD") in currencies && CurrencyCode("JPY") in currencies)
            assertTrue(data.transactions.any { it.amount.amountMinor > 100_000_000_000_000L })
            assertTrue(data.transactions.any { it.note.length > 500 })
            // Los cálculos no desbordan con importes enormes.
            FinanceCalculator(data, CurrencyCode.EUR, today).overview(YearMonth.from(today))
        }

    @Test
    fun `clear leaves default categories and the main account`() =
        runTest {
            val repository = seeded(DemoScenario.REGULAR)

            FinanceDemoDataGenerator(repository).clear()

            assertTrue(repository.current.transactions.isEmpty())
            assertEquals(DefaultCategory.entries.size, repository.current.categories.size)
        }
}
