package com.example.lifet.core.designsystem.util

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {
    private val eur = CurrencyCode.EUR
    private val jpy = CurrencyCode("JPY")

    @Test
    fun `parses comma and dot decimals`() {
        assertEquals(Money(1250, eur), parseAmount("12,5", eur))
        assertEquals(Money(1250, eur), parseAmount("12.50", eur))
        assertEquals(Money(123450, eur), parseAmount("1 234,5", eur))
    }

    @Test
    fun `rejects invalid and over-precise input`() {
        assertNull(parseAmount("", eur))
        assertNull(parseAmount("abc", eur))
        assertNull(parseAmount("-5", eur))
        assertNull(parseAmount("1,234", eur))
    }

    @Test
    fun `zero decimal currencies do not accept fractions`() {
        assertEquals(Money(500, jpy), parseAmount("500", jpy))
        assertNull(parseAmount("5,5", jpy))
    }

    @Test
    fun `overflow returns null instead of crashing`() {
        assertNull(parseAmount("99999999999999999999", eur))
    }

    @Test
    fun `sanitize keeps one separator and limits decimals`() {
        assertEquals("12,34", sanitizeAmountInput("12,345", eur))
        assertEquals("12,34", sanitizeAmountInput("1a2,3,4", eur))
        assertEquals("500", sanitizeAmountInput("500,5", jpy))
    }

    @Test
    fun `formats with locale and keeps the sign`() {
        val text = Money(-123456, eur).format(Locale.forLanguageTag("es-ES"))

        assertEquals("-1.234,56 €", text.replace(' ', ' '))
    }

    @Test
    fun `editable text has no symbol or sign`() {
        assertEquals("12.30", Money(-1230, eur).toEditableText())
    }
}
