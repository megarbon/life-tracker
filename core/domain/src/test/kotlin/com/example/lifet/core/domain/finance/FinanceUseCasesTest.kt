package com.example.lifet.core.domain.finance

import app.cash.turbine.test
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.Budget
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.Frequency
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.core.model.finance.defaultFinanceCategories
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.finance.InMemoryFinanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class FinanceUseCasesTest {
    // 10 de marzo de 2026, 10:00 en Madrid.
    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val today = LocalDate.of(2026, 3, 10)
    private val eur = CurrencyCode.EUR

    private val netflix =
        RecurringRule(
            id = "netflix",
            name = "Netflix",
            amount = Money(-1_299, eur),
            categoryId = DefaultCategory.SUBSCRIPTIONS.id,
            accountId = "bank",
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 15),
        )

    private fun tx(
        id: String,
        cents: Long,
        category: DefaultCategory,
        date: LocalDate,
    ) = FinanceTransaction(
        id = id,
        accountId = "bank",
        categoryId = category.id,
        amount = Money(cents, eur),
        date = date,
        createdAt = Instant.EPOCH,
        zone = clock.zone,
    )

    private fun repository(
        transactions: List<FinanceTransaction> = emptyList(),
        rules: List<RecurringRule> = emptyList(),
    ) = InMemoryFinanceRepository(
        FinanceDataSet(
            accounts = listOf(Account("bank", "Banco", AccountType.BANK, Money(50_000, eur))),
            categories = defaultFinanceCategories(),
            transactions = transactions,
            budgets = listOf(Budget(DefaultCategory.GROCERIES.id, Money(30_000, eur))),
            rules = rules,
        ),
    )

    @Test
    fun `today summary reacts to new movements`() =
        runTest {
            val repository = repository(listOf(tx("salary", 200_000, DefaultCategory.SALARY, today.withDayOfMonth(1))))

            ObserveFinanceTodaySummary(repository, clock)().test {
                val first = awaitItem()
                assertEquals(Money.zero(eur), first.spentToday)
                assertEquals(Money(200_000, eur), first.availableThisMonth)
                assertEquals(Money(30_000, eur), first.budgetRemaining)
                assertTrue(first.hasData)

                repository.saveTransaction(tx("lunch", -1_200, DefaultCategory.GROCERIES, today))

                val second = awaitItem()
                assertEquals(Money(1_200, eur), second.spentToday)
                assertEquals(Money(28_800, eur), second.budgetRemaining)
            }
        }

    @Test
    fun `today summary of an unused area reports no data`() =
        runTest {
            ObserveFinanceTodaySummary(InMemoryFinanceRepository(), clock)().test {
                assertFalse(awaitItem().hasData)
            }
        }

    @Test
    fun `series covers the requested range up to today`() =
        runTest {
            val repository = repository(listOf(tx("a", -1_000, DefaultCategory.LEISURE, today.minusDays(1))))
            val week = DateRange.containing(DateRangeUnit.WEEK, today)

            ObserveFinanceSeries(repository, clock)(week).test {
                val series = awaitItem()
                assertEquals(listOf(LocalDate.of(2026, 3, 9), today), series.days.map { it.date })
                assertEquals(Money(1_000, eur), series.totalSpent)
            }
        }

    @Test
    fun `sync generates past occurrences as real and future ones as planned, once`() =
        runTest {
            val repository = repository(rules = listOf(netflix))
            val sync = SyncRecurringTransactions(repository, clock)

            assertEquals(AppResult.Success(Unit), sync())
            sync()

            val dates = repository.current.transactions.map { it.date }.sorted()
            // El horizonte es hoy + 30 días (9 de abril): el 15 de abril aún no se genera.
            assertEquals(listOf("2026-01-15", "2026-02-15", "2026-03-15").map(LocalDate::parse), dates)
            assertEquals(1, repository.current.transactions.count { it.isPlannedAt(today) })
            assertEquals(LocalDate.of(2026, 4, 9), repository.current.rules.single().generatedThrough)
        }

    @Test
    fun `a deleted occurrence is not generated again`() =
        runTest {
            val repository = repository(rules = listOf(netflix))
            val sync = SyncRecurringTransactions(repository, clock)
            sync()

            repository.deleteTransaction("rec_netflix_2026-03-15")
            sync()

            assertFalse(repository.current.transactions.any { it.id == "rec_netflix_2026-03-15" })
        }

    @Test
    fun `overview shows the month with upcoming planned movements`() =
        runTest {
            val repository = repository(rules = listOf(netflix))
            SyncRecurringTransactions(repository, clock)()

            ObserveFinanceOverview(repository, clock)(YearMonth.of(2026, 3)).test {
                val overview = awaitItem()
                assertEquals(Money(1_299, eur), overview.summary.plannedSpent)
                assertEquals(listOf(LocalDate.of(2026, 3, 15)), overview.upcoming.map { it.date })
                assertEquals(6, overview.monthlyTotals.size)
                assertEquals(10, overview.balanceTrend.size)
            }
        }
}
