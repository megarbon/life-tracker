package com.example.lifet.core.model

/** Código de divisa ISO 4217 (tres letras mayúsculas), p. ej. `EUR`. */
@JvmInline
public value class CurrencyCode(
    public val value: String,
) {
    init {
        require(value.length == CODE_LENGTH && value.all { it in 'A'..'Z' }) {
            "Código de divisa ISO 4217 inválido: '$value'"
        }
    }

    override fun toString(): String = value

    public companion object {
        private const val CODE_LENGTH = 3
        public val EUR: CurrencyCode = CurrencyCode("EUR")
    }
}

/**
 * Importe monetario en unidades menores (céntimos) para no perder precisión con `Double`.
 * El signo lo lleva [amountMinor]: gastos negativos, ingresos positivos.
 *
 * Operar con divisas distintas es un error de programación, no un caso de negocio,
 * por eso [plus] y [minus] lanzan [IllegalArgumentException]; las conversiones son responsabilidad
 * de quien llama. Los desbordamientos de `Long` lanzan [ArithmeticException] en lugar de envolverse.
 */
public data class Money(
    public val amountMinor: Long,
    public val currency: CurrencyCode,
) : Comparable<Money> {
    public val isNegative: Boolean get() = amountMinor < 0
    public val isZero: Boolean get() = amountMinor == 0L

    public operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.addExact(amountMinor, other.amountMinor), currency)
    }

    public operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.subtractExact(amountMinor, other.amountMinor), currency)
    }

    public operator fun unaryMinus(): Money = Money(Math.negateExact(amountMinor), currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amountMinor.compareTo(other.amountMinor)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "No se pueden combinar $currency y ${other.currency} sin conversión"
        }
    }

    public companion object {
        public fun zero(currency: CurrencyCode): Money = Money(0L, currency)
    }
}
