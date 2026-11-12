package com.example.lifet.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode("USD")

    @Test
    fun `adding two amounts in the same currency sums the minor units`() {
        val result = Money(1_050, eur) + Money(250, eur)

        assertEquals(Money(1_300, eur), result)
    }

    @Test
    fun `subtracting past zero gives a negative amount`() {
        val result = Money(100, eur) - Money(350, eur)

        assertEquals(Money(-250, eur), result)
        assertTrue(result.isNegative)
    }

    @Test
    fun `adding different currencies is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Money(100, eur) + Money(100, usd) }
    }

    @Test
    fun `comparing different currencies is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Money(100, eur) < Money(100, usd) }
    }

    @Test
    fun `overflow throws instead of wrapping around`() {
        assertThrows(ArithmeticException::class.java) { Money(Long.MAX_VALUE, eur) + Money(1, eur) }
    }

    @Test
    fun `negating flips the sign and keeps the currency`() {
        assertEquals(Money(-500, eur), -Money(500, eur))
    }

    @Test
    fun `zero is neither negative nor positive`() {
        val zero = Money.zero(eur)

        assertTrue(zero.isZero)
        assertFalse(zero.isNegative)
    }

    @Test
    fun `currency code must be three uppercase letters`() {
        listOf("eur", "EU", "EURO", "E1R", "").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { CurrencyCode(invalid) }
        }
    }
}
