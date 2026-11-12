@file:Suppress("MagicNumber") // Fixtures de demo: importes y probabilidades literales a propósito.

package com.example.lifet.core.demo.finance

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.BANK
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.LONG_TEXT
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.OLD_ACCOUNT
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.OLD_CATEGORY
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.Budget
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.Frequency
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.core.model.finance.defaultFinanceCategories
import com.example.lifet.core.model.finance.pendingTransactions
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Random

private const val REGULAR_MONTHS = 6L
private const val NEW_USER_DAYS = 2L
private const val DAYS_PER_WEEK = 7
private const val OVER_BUDGET_MONTHS_AGO = 3L
private const val REFUND_MONTHS_AGO = 2L

/** Cuenta, categoría y reglas con ids fijos para que sembrar dos veces dé exactamente los mismos datos. */
private const val CASH = "demo_cash"
private const val SAVINGS = "demo_savings"
private const val PETS = "custom_pets"

/**
 * Construye los datos de demo de Finanzas. Es pura: todo el azar sale de [random] y todas las fechas son
 * relativas a [clock], así los mismos `(clock, semilla)` dan siempre los mismos datos.
 */
internal class FinanceDemoData(
    private val clock: Clock,
    private val random: Random,
) : DemoLedgerBuilder(clock.zone, random) {
    private val today: LocalDate = LocalDate.now(clock)
    private val eur = CurrencyCode.EUR
    private val edge by lazy { EdgeCaseDemoData(clock, random) }

    fun build(scenario: DemoScenario): FinanceDataSet =
        when (scenario) {
            DemoScenario.EMPTY -> empty()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> regular()
            DemoScenario.EDGE_CASES -> edgeCases()
        }

    private fun empty() =
        FinanceDataSet(
            accounts = listOf(bankAccount(opening = 0L)),
            categories = defaultFinanceCategories(),
        )

    private fun newUser(): FinanceDataSet {
        val txs =
            listOf(
                expense(DefaultCategory.GROCERIES, today.minusDays(NEW_USER_DAYS), 3_875, "Mercadona"),
                expense(DefaultCategory.RESTAURANTS, today.minusDays(1), 1_450, "Menú del día"),
                expense(DefaultCategory.TRANSPORT, today, 250, "Metro"),
            )
        return FinanceDataSet(
            accounts = listOf(bankAccount(opening = 85_000L)),
            categories = defaultFinanceCategories(),
            transactions = txs,
        )
    }

    private fun regular(): FinanceDataSet {
        val firstMonth = YearMonth.from(today).minusMonths(REGULAR_MONTHS)
        val start = firstMonth.atDay(1)
        val rules = regularRules(start)
        val horizon = maxOf(YearMonth.from(today).atEndOfMonth(), today.plusDays(FinanceCalculator.UPCOMING_DAYS))
        val recurring =
            rules.flatMap { it.pendingTransactions(start, horizon, instantAt(today, 0, 0), zone) }
        val manual = mutableListOf<FinanceTransaction>()
        var day = start
        while (day <= today) {
            manual += dayExpenses(day)
            day = day.plusDays(1)
        }
        manual += monthlySpecials()
        val categories = defaultFinanceCategories() + FinanceCategory(PETS, CategoryKind.EXPENSE, "pets", "Mascota")
        return FinanceDataSet(
            accounts =
                listOf(
                    bankAccount(opening = 120_000L),
                    Account(CASH, "Efectivo", AccountType.CASH, Money(8_000, eur), sortOrder = 1),
                    Account(SAVINGS, "Ahorro", AccountType.SAVINGS, Money(300_000, eur), sortOrder = 2),
                ),
            categories = categories,
            transactions = (recurring + manual).sortedBy { it.date },
            transfers = regularTransfers(start),
            budgets = regularBudgets(),
            rules = rules.map { it.copy(generatedThrough = horizon) },
        )
    }

    private fun regularRules(start: LocalDate): List<RecurringRule> =
        listOf(
            rule("payroll", "Nómina", 210_000, DefaultCategory.SALARY, start.withDayOfMonth(1)),
            rule("rent", "Alquiler", -75_000, DefaultCategory.HOUSING, start.withDayOfMonth(5)),
            rule("netflix", "Netflix", -1_299, DefaultCategory.SUBSCRIPTIONS, start.withDayOfMonth(15)),
            rule("spotify", "Spotify", -1_099, DefaultCategory.SUBSCRIPTIONS, start.withDayOfMonth(20)),
            rule("gym", "Gimnasio", -3_490, DefaultCategory.SUBSCRIPTIONS, start.withDayOfMonth(3)),
        )

    private fun rule(
        id: String,
        name: String,
        cents: Long,
        category: DefaultCategory,
        startDate: LocalDate,
    ) = RecurringRule(
        id = id,
        name = name,
        amount = Money(cents, eur),
        categoryId = category.id,
        accountId = BANK,
        frequency = Frequency.MONTHLY,
        startDate = startDate,
    )

    /** Supermercado 2-3 veces por semana, ocio en fin de semana y algún gasto suelto. */
    private fun dayExpenses(day: LocalDate): List<FinanceTransaction> {
        val weekday = day.dayOfWeek.value
        val result = mutableListOf<FinanceTransaction>()
        val groceryDay = weekday == 1 || weekday == 4 || (weekday == 6 && random.nextInt(2) == 0)
        if (groceryDay) {
            result += expense(DefaultCategory.GROCERIES, day, randomCents(2_000, 6_500), pick(GROCERY_NOTES))
        }
        if (weekday >= 6) {
            if (random.nextInt(10) < 6) {
                result += expense(DefaultCategory.LEISURE, day, randomCents(1_500, 4_500), pick(LEISURE_NOTES))
            }
            if (random.nextInt(10) < 4) {
                result += expense(DefaultCategory.RESTAURANTS, day, randomCents(1_800, 3_800), pick(RESTAURANT_NOTES))
            }
        } else if (random.nextInt(DAYS_PER_WEEK * 2) == 0) {
            result += expense(DefaultCategory.TRANSPORT, day, randomCents(200, 1_200), pick(TRANSPORT_NOTES))
        }
        if (random.nextInt(DAYS_PER_WEEK * 6) == 0) {
            result += expense(DefaultCategory.HEALTH, day, randomCents(800, 2_800), "Farmacia")
        }
        if (random.nextInt(DAYS_PER_WEEK * 5) == 0) {
            result += spend(PETS, day, randomCents(900, 3_200), "Pienso 🐶")
        }
        return result
    }

    /** Un mes con presupuesto superado, una devolución y un reembolso de ocio. */
    private fun monthlySpecials(): List<FinanceTransaction> {
        val overMonth = YearMonth.from(today).minusMonths(OVER_BUDGET_MONTHS_AGO)
        val refundMonth = YearMonth.from(today).minusMonths(REFUND_MONTHS_AGO)
        return listOf(
            expense(DefaultCategory.LEISURE, overMonth.atDay(12), 12_000, "Concierto 🎸"),
            expense(DefaultCategory.LEISURE, overMonth.atDay(19), 7_500, "Escape room"),
            expense(DefaultCategory.GROCERIES, overMonth.atDay(23), 9_800, "Compra grande para la cena"),
            expense(DefaultCategory.SHOPPING, refundMonth.atDay(8), 5_990, "Zapatillas"),
            income(DefaultCategory.SHOPPING, refundMonth.atDay(14), 5_990, "Devolución zapatillas"),
        )
    }

    private fun regularTransfers(start: LocalDate): List<FinanceTransfer> {
        val months =
            generateSequence(YearMonth.from(start)) { it.plusMonths(1) }.takeWhile {
                it <=
                    YearMonth.from(today)
            }
        val savings =
            months
                .map { it.atDay(2) }
                .filter { it <= today }
                .map { transfer(BANK, SAVINGS, 20_000, it, "Ahorro mensual") }
                .toList()
        val cash = listOf(transfer(BANK, CASH, 6_000, today.minusDays(9), "Cajero"))
        return savings + cash
    }

    private fun regularBudgets() =
        listOf(
            Budget(DefaultCategory.GROCERIES.id, Money(35_000, eur)),
            Budget(DefaultCategory.LEISURE.id, Money(15_000, eur)),
            Budget(DefaultCategory.RESTAURANTS.id, Money(12_000, eur)),
            Budget(DefaultCategory.TRANSPORT.id, Money(6_000, eur)),
            Budget(DefaultCategory.SUBSCRIPTIONS.id, Money(7_000, eur)),
        )

    /** Divisas distintas, importes enormes, medianoche, cambio de hora, textos larguísimos y huecos sin datos. */
    private fun edgeCases(): FinanceDataSet {
        val categories =
            defaultFinanceCategories() +
                FinanceCategory(
                    OLD_CATEGORY,
                    CategoryKind.EXPENSE,
                    "category",
                    "Antigua 🗄️",
                    archived = true,
                    sortOrder = 99,
                )
        return FinanceDataSet(
            accounts = edge.accounts(),
            categories = categories,
            transactions = edge.transactions(),
            transfers = listOf(transfer(BANK, OLD_ACCOUNT, 500, today.minusDays(10), "Traspaso " + LONG_TEXT)),
            budgets =
                listOf(
                    Budget(DefaultCategory.LEISURE.id, Money(0, eur)),
                    Budget(DefaultCategory.HOUSING.id, Money(100_000, eur)),
                    Budget(DefaultCategory.HEALTH.id, Money(5_000, eur)),
                ),
        )
    }

    private companion object {
        val GROCERY_NOTES = listOf("Mercadona", "Carrefour", "Lidl", "Frutería", "Dia", "Panadería 🥖")
        val LEISURE_NOTES = listOf("Cine 🎬", "Cervezas", "Bolera", "Museo", "Partido de fútbol ⚽")
        val RESTAURANT_NOTES = listOf("Cena con amigos", "Brunch 🥞", "Pizzería", "Tapas", "Sushi 🍣")
        val TRANSPORT_NOTES = listOf("Metro", "Bus", "Taxi", "Gasolina")
    }
}
