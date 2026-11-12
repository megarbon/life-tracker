@file:Suppress("MagicNumber") // Fixtures de demo: importes y probabilidades literales a propósito.

package com.example.lifet.core.demo.finance

import com.example.lifet.core.demo.DemoScenario
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
private const val HUGE_AMOUNT = 123_456_789_012_345L
private const val LONG_NOTE_CHARS = 800
private const val OVER_BUDGET_MONTHS_AGO = 3L
private const val REFUND_MONTHS_AGO = 2L

/** Cuenta, categoría y reglas con ids fijos para que sembrar dos veces dé exactamente los mismos datos. */
private const val BANK = Account.DEFAULT_ID
private const val CASH = "demo_cash"
private const val SAVINGS = "demo_savings"
private const val USD_ACCOUNT = "demo_usd"
private const val JPY_ACCOUNT = "demo_jpy"
private const val OLD_ACCOUNT = "demo_old"
private const val PETS = "custom_pets"
private const val OLD_CATEGORY = "custom_old"

/**
 * Construye los datos de demo de Finanzas. Es pura: todo el azar sale de [random] y todas las fechas son
 * relativas a [clock], así los mismos `(clock, semilla)` dan siempre los mismos datos.
 */
internal class FinanceDemoData(
    private val clock: Clock,
    private val random: Random,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)
    private val eur = CurrencyCode.EUR
    private var counter = 0

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
        val months = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }.takeWhile { it <= YearMonth.from(today) }
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
        val usd = CurrencyCode("USD")
        val jpy = CurrencyCode("JPY")
        val txs = mutableListOf<FinanceTransaction>()
        txs += expense(DefaultCategory.OTHER_EXPENSE, today, 1, "Un céntimo")
        txs += income(DefaultCategory.OTHER_INCOME, today.minusDays(3), HUGE_AMOUNT, "Premio enorme 🎰")
        txs += expense(DefaultCategory.HOUSING, today.minusDays(4), HUGE_AMOUNT / 2, "Compra de un castillo")
        txs += expense(DefaultCategory.SHOPPING, today.minusDays(5), 4_999, "x".repeat(LONG_NOTE_CHARS))
        txs += expense(DefaultCategory.LEISURE, today.minusDays(6), 2_300, "Cena 🍣🍜🥢 con 日本語 y العربية " + LONG_TEXT)
        txs += expense(DefaultCategory.GROCERIES, today.minusDays(7), 3_100, "")
        txs += midnightPair()
        txs += daylightSavingPair()
        txs += expense(DefaultCategory.RESTAURANTS, today.minusDays(40), 2_500, "Tras 20 días sin datos")
        txs += expense(DefaultCategory.TRANSPORT, today.plusDays(2), 1_500, "Previsto: taxi aeropuerto")
        txs += expense(DefaultCategory.SUBSCRIPTIONS, today.plusDays(45), 999, "Previsto lejano")
        txs += expense(DefaultCategory.SHOPPING, today.minusDays(2), 129_900, "Portátil", account = USD_ACCOUNT, currency = usd)
        txs += expense(DefaultCategory.RESTAURANTS, today.minusDays(1), 1_800_000, "Ramen en Tokio", account = JPY_ACCOUNT, currency = jpy)
        txs += spend(OLD_CATEGORY, today.minusDays(60), 1_000, "Categoría archivada", account = OLD_ACCOUNT)
        val categories =
            defaultFinanceCategories() +
                FinanceCategory(OLD_CATEGORY, CategoryKind.EXPENSE, "category", "Antigua 🗄️", archived = true, sortOrder = 99)
        return FinanceDataSet(
            accounts =
                listOf(
                    bankAccount(opening = -5_000L),
                    Account(USD_ACCOUNT, "Cuenta EE. UU.", AccountType.BANK, Money(250_000, usd), sortOrder = 1),
                    Account(JPY_ACCOUNT, "Yenes", AccountType.CASH, Money(5_000_000, jpy), sortOrder = 2),
                    Account(OLD_ACCOUNT, "Cuenta cerrada", AccountType.CARD, Money.zero(eur), archived = true, sortOrder = 3),
                ),
            categories = categories,
            transactions = txs,
            transfers = listOf(transfer(BANK, OLD_ACCOUNT, 500, today.minusDays(10), "Traspaso " + LONG_TEXT)),
            budgets =
                listOf(
                    Budget(DefaultCategory.LEISURE.id, Money(0, eur)),
                    Budget(DefaultCategory.HOUSING.id, Money(100_000, eur)),
                    Budget(DefaultCategory.HEALTH.id, Money(5_000, eur)),
                ),
        )
    }

    /** Dos movimientos a ambos lados de la medianoche local: 23:59:59 y 00:00:01 del día siguiente. */
    private fun midnightPair(): List<FinanceTransaction> {
        val day = today.minusDays(9)
        return listOf(
            expense(DefaultCategory.LEISURE, day, 1_800, "Última copa", createdAt = instantAt(day, 23, 59, 59)),
            expense(DefaultCategory.TRANSPORT, day.plusDays(1), 2_200, "Taxi de vuelta", createdAt = instantAt(day.plusDays(1), 0, 0, 1)),
        )
    }

    /** Si la zona tiene cambio de hora, pone movimientos justo antes y después de la última transición. */
    private fun daylightSavingPair(): List<FinanceTransaction> {
        val transition = zone.rules.previousTransition(clock.instant()) ?: return emptyList()
        val before = transition.instant.minusSeconds(ONE_HOUR)
        val after = transition.instant.plusSeconds(ONE_HOUR)
        return listOf(
            expense(DefaultCategory.LEISURE, before.atZone(zone).toLocalDate(), 1_100, "Antes del cambio de hora", createdAt = before),
            expense(DefaultCategory.LEISURE, after.atZone(zone).toLocalDate(), 1_200, "Después del cambio de hora", createdAt = after),
        )
    }

    private fun bankAccount(opening: Long) = Account(BANK, "Cuenta corriente", AccountType.BANK, Money(opening, eur))

    private fun spend(
        categoryId: String,
        date: LocalDate,
        cents: Long,
        note: String,
        account: String = BANK,
        currency: CurrencyCode = eur,
        createdAt: Instant = instantAt(date, DEFAULT_HOUR + random.nextInt(HOURS_SPREAD), random.nextInt(MINUTES)),
    ) = transaction(categoryId, date, Money(-cents, currency), note, account, createdAt)

    private fun expense(
        category: DefaultCategory,
        date: LocalDate,
        cents: Long,
        note: String,
        account: String = BANK,
        currency: CurrencyCode = eur,
        createdAt: Instant = instantAt(date, DEFAULT_HOUR + random.nextInt(HOURS_SPREAD), random.nextInt(MINUTES)),
    ) = spend(category.id, date, cents, note, account, currency, createdAt)

    private fun income(
        category: DefaultCategory,
        date: LocalDate,
        cents: Long,
        note: String,
    ) = transaction(
        category.id,
        date,
        Money(cents, eur),
        note,
        BANK,
        instantAt(date, DEFAULT_HOUR + random.nextInt(HOURS_SPREAD), random.nextInt(MINUTES)),
    )

    private fun transaction(
        categoryId: String,
        date: LocalDate,
        amount: Money,
        note: String,
        account: String,
        createdAt: Instant,
    ) = FinanceTransaction(
        id = "demo_tx_${counter++}",
        accountId = account,
        categoryId = categoryId,
        amount = amount,
        date = date,
        note = note,
        createdAt = createdAt,
        zone = zone,
    )

    private fun transfer(
        from: String,
        to: String,
        cents: Long,
        date: LocalDate,
        note: String,
    ) = FinanceTransfer(
        id = "demo_tr_${counter++}",
        fromAccountId = from,
        toAccountId = to,
        amount = Money(cents, eur),
        date = date,
        note = note,
        createdAt = instantAt(date, TRANSFER_HOUR, 0),
        zone = zone,
    )

    private fun instantAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Instant = date.atTime(hour, minute, second).atZone(zone).toInstant()

    private fun randomCents(
        min: Int,
        max: Int,
    ): Long = (min + random.nextInt(max - min + 1)).toLong()

    private fun pick(options: List<String>): String = options[random.nextInt(options.size)]

    private companion object {
        const val DEFAULT_HOUR = 9
        const val HOURS_SPREAD = 12
        const val MINUTES = 60
        const val TRANSFER_HOUR = 8
        const val ONE_HOUR = 3_600L
        const val LONG_TEXT =
            "Una nota extraordinariamente larga para comprobar que la lista, la hoja de edición y los " +
                "resúmenes no se rompen cuando el usuario pega un párrafo entero en el campo de nota, " +
                "incluso con la fuente al doscientos por ciento y en pantallas estrechas."
        val GROCERY_NOTES = listOf("Mercadona", "Carrefour", "Lidl", "Frutería", "Dia", "Panadería 🥖")
        val LEISURE_NOTES = listOf("Cine 🎬", "Cervezas", "Bolera", "Museo", "Partido de fútbol ⚽")
        val RESTAURANT_NOTES = listOf("Cena con amigos", "Brunch 🥞", "Pizzería", "Tapas", "Sushi 🍣")
        val TRANSPORT_NOTES = listOf("Metro", "Bus", "Taxi", "Gasolina")
    }
}
