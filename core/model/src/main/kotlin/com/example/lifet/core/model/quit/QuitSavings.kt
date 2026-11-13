package com.example.lifet.core.model.quit

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import java.math.BigInteger
import java.time.Instant

/**
 * Dinero que no se ha gastado desde [QuitHabit.startedAt] hasta [at], dado que desde entonces se han consumido
 * [consumedUnits] unidades. Es lo que habría costado el consumo previo menos lo que de verdad se ha consumido, con
 * un suelo en cero: consumir más que antes no resta de lo que ya se ahorró en otro momento.
 *
 * Aritmética exacta con enteros grandes (sin `Double`) y saturada en `Long.MAX_VALUE`, de modo que un precio o un
 * consumo enormes no desbordan.
 */
public fun QuitHabit.savedAt(
    at: Instant,
    consumedUnits: Long,
): Money {
    val elapsed = (at.epochSecond - startedAt.epochSecond).coerceAtLeast(0L)
    val period = baselinePeriod.seconds
    val expectedScaled = BigInteger.valueOf(baselineUnits.toLong()) * BigInteger.valueOf(elapsed)
    val consumedScaled = BigInteger.valueOf(consumedUnits.coerceAtLeast(0L)) * BigInteger.valueOf(period)
    val avoidedScaled = expectedScaled - consumedScaled
    if (avoidedScaled.signum() <= 0) return Money.zero(packPrice.currency)
    val price = BigInteger.valueOf(packPrice.amountMinor.coerceAtLeast(0L))
    val divisor = BigInteger.valueOf(unitsPerPack.coerceAtLeast(1).toLong()) * BigInteger.valueOf(period)
    return Money((price * avoidedScaled / divisor).saturatingLong(), packPrice.currency)
}

/** Suma de importes de la misma divisa saturada en `Long.MAX_VALUE`, para que un total enorme no lance. */
public fun sumSaturating(
    currency: CurrencyCode,
    amounts: List<Money>,
): Money {
    val total = amounts.fold(BigInteger.ZERO) { acc, money -> acc + BigInteger.valueOf(money.amountMinor) }
    return Money(total.saturatingLong(), currency)
}

private fun BigInteger.saturatingLong(): Long =
    when {
        this > BigInteger.valueOf(Long.MAX_VALUE) -> Long.MAX_VALUE
        this < BigInteger.valueOf(Long.MIN_VALUE) -> Long.MIN_VALUE
        else -> toLong()
    }
