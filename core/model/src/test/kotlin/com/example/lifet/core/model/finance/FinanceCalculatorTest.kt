package com.example.lifet.core.model.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class FinanceCalculatorTest {
    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode("USD")
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 3, 10)
    private val march = YearMonth.of(2026, 3)
    private var counter = 0

    private val bank = Account("bank", "Banco", AccountType.BANK, Money(100_000, eur))
    private val cash = Account("cash", "Efectivo", AccountType.CASH, Money(5_000, eur))
    private val dollars = Account("usd", "Dólares", AccountType.BANK, Money(1_000, usd))

    private fun tx(
        cents: Long,
        category: DefaultCategory,
        date: LocalDate,
        account: String = "bank",
        currency: CurrencyCode = eur,
        note: String = "",
    ) = FinanceTransaction(
        id = "t${counter++}",
        accountId = account,
        categoryId = category.id,
        amount = Money(cents, currency),
        date = date,
        note = note,
        createdAt = Instant.EPOCH.plusSeconds(counter.toLong()),
        zone = zone,
    )

    private fun calculator(
        transactions: List<FinanceTransaction>,
        budgets: List<Budget> = emptyList(),
        transfers: List<FinanceTransfer> = emptyList(),
    ) = FinanceCalculator(
        FinanceDataSet(
            accounts = listOf(bank, cash, dollars),
            categories = defaultFinanceCategories(),
            transactions = transactions,
            transfers = transfers,
            budgets = budgets,
        ),
        eur,
        today,
    )

    @Test
    fun `month summary nets refunds and separates planned movements`() {
        val calc =
            calculator(
                listOf(
                    tx(250_000, DefaultCategory.SALARY, march.atDay(1)),
                    tx(-6_000, DefaultCategory.GROCERIES, march.atDay(3)),
                    tx(1_500, DefaultCategory.GROCERIES, march.atDay(4)), // devolución
                    tx(-1_299, DefaultCategory.SUBSCRIPTIONS, march.atDay(20)), // prevista
                    tx(-9_999, DefaultCategory.GROCERIES, march.atDay(5), currency = usd, account = "usd"),
                ),
                budgets = listOf(Budget(DefaultCategory.GROCERIES.id, Money(4_000, eur))),
            )

        val summary = calc.monthSummary(march)

        assertEquals(Money(250_000, eur), summary.income)
        assertEquals(Money(4_500, eur), summary.spent)
        assertEquals(Money(1_299, eur), summary.plannedSpent)
        assertEquals(Money(250_000 - 4_500 - 1_299, eur), summary.available)
        val groceries = summary.byCategory.first { it.category.id == DefaultCategory.GROCERIES.id }
        assertEquals(Money(4_500, eur), groceries.spent)
        assertTrue(groceries.isOver)
        assertEquals(Money(-500, eur), summary.budgetRemaining)
        assertEquals(setOf(usd), calc.otherCurrencies)
    }

    @Test
    fun `month without budgets has no budget total`() {
        val summary = calculator(listOf(tx(-1_000, DefaultCategory.LEISURE, today))).monthSummary(march)

        assertNull(summary.budgetTotal)
        assertNull(summary.budgetRemaining)
    }

    @Test
    fun `budget without spending shows as zero spent`() {
        val calc = calculator(emptyList(), budgets = listOf(Budget(DefaultCategory.HEALTH.id, Money(3_000, eur))))

        val health = calc.monthSummary(march).byCategory.single()

        assertEquals(Money.zero(eur), health.spent)
        assertEquals(0f, health.ratio)
        assertFalse(health.isOver)
    }

    @Test
    fun `transfers move account balances but not the total or the spending`() {
        val transfer =
            FinanceTransfer("tr", "bank", "cash", Money(2_000, eur), today, createdAt = Instant.EPOCH, zone = zone)
        val calc =
            calculator(
                listOf(tx(-1_000, DefaultCategory.LEISURE, today, account = "cash")),
                transfers = listOf(transfer),
            )

        val balances = calc.accountBalances().associate { it.account.id to it.balance }

        assertEquals(Money(98_000, eur), balances["bank"])
        assertEquals(Money(6_000, eur), balances["cash"])
        assertEquals(Money(1_000, usd), balances["usd"])
        assertEquals(Money(104_000, eur), calc.totalBalance())
        assertEquals(Money(1_000, eur), calc.monthSummary(march).spent)
    }

    @Test
    fun `daily balances accumulate from the opening balance and stop at today`() {
        val calc =
            calculator(
                listOf(
                    tx(-1_000, DefaultCategory.LEISURE, today.minusDays(2)),
                    tx(-500, DefaultCategory.LEISURE, today),
                    tx(-99_000, DefaultCategory.HOUSING, today.plusDays(5)),
                ),
            )

        val balances = calc.dailyBalances(today.minusDays(3), today.plusDays(10))

        assertEquals(4, balances.size)
        assertEquals(Money(105_000, eur), balances.first().balance)
        assertEquals(Money(103_500, eur), balances.last().balance)
    }

    @Test
    fun `month end balances use today for the current month`() {
        val calc = calculator(listOf(tx(-1_000, DefaultCategory.LEISURE, march.minusMonths(1).atDay(15))))

        val balances = calc.monthEndBalances(march, 3)

        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), today), balances.map { it.date })
        // Saldo inicial 105.000 (banco + efectivo); el gasto de 1.000 es del 15 de febrero.
        assertEquals(listOf(105_000L, 104_000L, 104_000L), balances.map { it.balance.amountMinor })
    }

    @Test
    fun `ledger groups by day newest first with the daily net total`() {
        val calc =
            calculator(
                listOf(
                    tx(-1_000, DefaultCategory.LEISURE, march.atDay(2)),
                    tx(-2_000, DefaultCategory.GROCERIES, march.atDay(2)),
                    tx(-300, DefaultCategory.TRANSPORT, march.atDay(5)),
                    tx(-300, DefaultCategory.TRANSPORT, march.minusMonths(1).atDay(5)),
                ),
            )

        val ledger = calc.ledger(march)

        assertEquals(listOf(march.atDay(5), march.atDay(2)), ledger.map { it.date })
        assertEquals(Money(-3_000, eur), ledger.last().total)
    }

    @Test
    fun `text search covers the whole history and filters by kind`() {
        val calc =
            calculator(
                listOf(
                    tx(-1_000, DefaultCategory.LEISURE, march.atDay(2), note = "Cine 🎬"),
                    tx(-2_000, DefaultCategory.LEISURE, march.minusMonths(2).atDay(2), note = "cine con amigos"),
                    tx(250_000, DefaultCategory.SALARY, march.atDay(1), note = "Nómina"),
                ),
            )

        assertEquals(2, calc.ledger(march, LedgerFilter(query = "CINE")).sumOf { it.entries.size })
        val incomes = calc.ledger(march, LedgerFilter(kind = CategoryKind.INCOME))
        assertEquals(1, incomes.sumOf { it.entries.size })
    }

    @Test
    fun `monthly totals return the requested months oldest first, including empty ones`() {
        val calc =
            calculator(
                listOf(
                    tx(-1_000, DefaultCategory.LEISURE, march.atDay(2)),
                    tx(-4_000, DefaultCategory.LEISURE, march.minusMonths(2).atDay(2)),
                ),
            )

        val totals = calc.monthlyTotals(march, 3)

        assertEquals(listOf(march.minusMonths(2), march.minusMonths(1), march), totals.map { it.month })
        assertEquals(listOf(4_000L, 0L, 1_000L), totals.map { it.spent.amountMinor })
    }

    @Test
    fun `today summary and upcoming use the planned split`() {
        val calc =
            calculator(
                listOf(
                    tx(-700, DefaultCategory.RESTAURANTS, today),
                    tx(-300, DefaultCategory.TRANSPORT, today),
                    tx(-1_299, DefaultCategory.SUBSCRIPTIONS, today.plusDays(3)),
                    tx(-1_299, DefaultCategory.SUBSCRIPTIONS, today.plusDays(60)),
                ),
            )

        val summary = calc.todaySummary()

        assertEquals(Money(1_000, eur), summary.spentToday)
        assertEquals(Money(-2_299, eur), summary.availableThisMonth)
        assertEquals(1, calc.upcoming().size)
    }

    @Test
    fun `series covers the past days of the range with income, spending and balance`() {
        val calc =
            calculator(
                listOf(
                    tx(250_000, DefaultCategory.SALARY, march.atDay(1)),
                    tx(-1_000, DefaultCategory.LEISURE, march.atDay(2)),
                ),
            )

        val series = calc.series(DateRange(DateRangeUnit.MONTH, march.atDay(1)))

        assertEquals(10, series.days.size)
        assertEquals(Money(250_000, eur), series.totalIncome)
        assertEquals(Money(1_000, eur), series.totalSpent)
        assertEquals(Money(354_000, eur), series.days.last().balance)
    }
}
