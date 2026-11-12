package com.example.lifet.core.demo.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Random

/**
 * Construye movimientos y transferencias de demo con ids secuenciales y horas aleatorias dentro del día.
 * Comparte [random] con el generador para que toda la secuencia salga de la misma semilla.
 */
internal open class DemoLedgerBuilder(
    protected val zone: ZoneId,
    private val random: Random,
) {
    private val eur = CurrencyCode.EUR
    private var counter = 0

    protected fun bankAccount(opening: Long) = Account(BANK, "Cuenta corriente", AccountType.BANK, Money(opening, eur))

    protected fun spend(
        categoryId: String,
        date: LocalDate,
        cents: Long,
        note: String,
        account: String = BANK,
        currency: CurrencyCode = eur,
        createdAt: Instant = instantAt(date, DEFAULT_HOUR + random.nextInt(HOURS_SPREAD), random.nextInt(MINUTES)),
    ) = transaction(categoryId, date, Money(-cents, currency), note, account, createdAt)

    protected fun expense(
        category: DefaultCategory,
        date: LocalDate,
        cents: Long,
        note: String,
        account: String = BANK,
        currency: CurrencyCode = eur,
        createdAt: Instant = instantAt(date, DEFAULT_HOUR + random.nextInt(HOURS_SPREAD), random.nextInt(MINUTES)),
    ) = spend(category.id, date, cents, note, account, currency, createdAt)

    protected fun income(
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

    protected fun transaction(
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

    protected fun transfer(
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

    protected fun instantAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Instant = date.atTime(hour, minute, second).atZone(zone).toInstant()

    protected fun randomCents(
        min: Int,
        max: Int,
    ): Long = (min + random.nextInt(max - min + 1)).toLong()

    protected fun pick(options: List<String>): String = options[random.nextInt(options.size)]

    companion object {
        const val BANK = Account.DEFAULT_ID
        const val DEFAULT_HOUR = 9
        const val HOURS_SPREAD = 12
        const val MINUTES = 60
        const val TRANSFER_HOUR = 8
        const val ONE_HOUR = 3_600L
        const val HUGE_AMOUNT = 123_456_789_012_345L
        const val LONG_NOTE_CHARS = 800
        const val USD_ACCOUNT = "demo_usd"
        const val JPY_ACCOUNT = "demo_jpy"
        const val OLD_ACCOUNT = "demo_old"
        const val OLD_CATEGORY = "custom_old"
        const val LONG_TEXT =
            "Una nota extraordinariamente larga para comprobar que la lista, la hoja de edición y los " +
                "resúmenes no se rompen cuando el usuario pega un párrafo entero en el campo de nota, " +
                "incluso con la fuente al doscientos por ciento y en pantallas estrechas."
    }
}
