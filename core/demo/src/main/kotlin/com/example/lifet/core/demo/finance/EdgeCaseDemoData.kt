@file:Suppress("MagicNumber") // Fixtures de demo: importes literales a propósito.

package com.example.lifet.core.demo.finance

import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.HUGE_AMOUNT
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.JPY_ACCOUNT
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.LONG_NOTE_CHARS
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.LONG_TEXT
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.OLD_ACCOUNT
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.OLD_CATEGORY
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.ONE_HOUR
import com.example.lifet.core.demo.finance.DemoLedgerBuilder.Companion.USD_ACCOUNT
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceTransaction
import java.time.Clock
import java.time.LocalDate
import java.util.Random

/** Casos límite de Finanzas: otras divisas, importes enormes, textos larguísimos, medianoche y cambio de hora. */
internal class EdgeCaseDemoData(
    private val clock: Clock,
    random: Random,
) : DemoLedgerBuilder(clock.zone, random) {
    private val today: LocalDate = LocalDate.now(clock)
    private val eur = CurrencyCode.EUR

    fun accounts(): List<Account> =
        listOf(
            bankAccount(opening = -5_000L),
            Account(
                USD_ACCOUNT,
                "Cuenta EE. UU.",
                AccountType.BANK,
                Money(250_000, CurrencyCode("USD")),
                sortOrder = 1,
            ),
            Account(JPY_ACCOUNT, "Yenes", AccountType.CASH, Money(5_000_000, CurrencyCode("JPY")), sortOrder = 2),
            Account(OLD_ACCOUNT, "Cuenta cerrada", AccountType.CARD, Money.zero(eur), archived = true, sortOrder = 3),
        )

    fun transactions(): List<FinanceTransaction> {
        val usd = CurrencyCode("USD")
        val jpy = CurrencyCode("JPY")
        return listOf(
            expense(DefaultCategory.OTHER_EXPENSE, today, 1, "Un céntimo"),
            income(DefaultCategory.OTHER_INCOME, today.minusDays(3), HUGE_AMOUNT, "Premio enorme 🎰"),
            expense(DefaultCategory.HOUSING, today.minusDays(4), HUGE_AMOUNT / 2, "Compra de un castillo"),
            expense(DefaultCategory.SHOPPING, today.minusDays(5), 4_999, "x".repeat(LONG_NOTE_CHARS)),
            expense(DefaultCategory.LEISURE, today.minusDays(6), 2_300, "Cena 🍣🍜🥢 con 日本語 y العربية " + LONG_TEXT),
            expense(DefaultCategory.GROCERIES, today.minusDays(7), 3_100, ""),
            expense(DefaultCategory.RESTAURANTS, today.minusDays(40), 2_500, "Tras 20 días sin datos"),
            expense(DefaultCategory.TRANSPORT, today.plusDays(2), 1_500, "Previsto: taxi aeropuerto"),
            expense(DefaultCategory.SUBSCRIPTIONS, today.plusDays(45), 999, "Previsto lejano"),
            expense(DefaultCategory.SHOPPING, today.minusDays(2), 129_900, "Portátil", USD_ACCOUNT, usd),
            expense(DefaultCategory.RESTAURANTS, today.minusDays(1), 1_800_000, "Ramen en Tokio", JPY_ACCOUNT, jpy),
            spend(OLD_CATEGORY, today.minusDays(60), 1_000, "Categoría archivada", OLD_ACCOUNT),
        ) + midnightPair() + daylightSavingPair()
    }

    /** Dos movimientos a ambos lados de la medianoche local: 23:59:59 y 00:00:01 del día siguiente. */
    private fun midnightPair(): List<FinanceTransaction> {
        val day = today.minusDays(9)
        return listOf(
            expense(DefaultCategory.LEISURE, day, 1_800, "Última copa", createdAt = instantAt(day, 23, 59, 59)),
            expense(
                DefaultCategory.TRANSPORT,
                day.plusDays(1),
                2_200,
                "Taxi de vuelta",
                createdAt = instantAt(day.plusDays(1), 0, 0, 1),
            ),
        )
    }

    /** Si la zona tiene cambio de hora, pone movimientos justo antes y después de la última transición. */
    private fun daylightSavingPair(): List<FinanceTransaction> {
        val transition = zone.rules.previousTransition(clock.instant()) ?: return emptyList()
        val before = transition.instant.minusSeconds(ONE_HOUR)
        val after = transition.instant.plusSeconds(ONE_HOUR)
        return listOf(
            expense(
                DefaultCategory.LEISURE,
                before.atZone(zone).toLocalDate(),
                1_100,
                "Antes del cambio de hora",
                createdAt = before,
            ),
            expense(
                DefaultCategory.LEISURE,
                after.atZone(zone).toLocalDate(),
                1_200,
                "Después del cambio de hora",
                createdAt = after,
            ),
        )
    }
}
