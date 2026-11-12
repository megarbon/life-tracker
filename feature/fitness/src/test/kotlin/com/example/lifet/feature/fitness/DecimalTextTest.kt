package com.example.lifet.feature.fitness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalTextTest {
    @Test
    fun `decimal comma and dot both parse to the same scaled value`() {
        assertEquals(82_500L, parseScaled("82,5", 3))
        assertEquals(82_500L, parseScaled("82.5", 3))
        assertEquals(82_000L, parseScaled("82", 3))
        assertEquals(500L, parseScaled(",5", 3))
    }

    @Test
    fun `spaces are ignored and trailing zeros do not count as extra decimals`() {
        assertEquals(1_234_000L, parseScaled("1 234", 3))
        assertEquals(62_500L, parseScaled("62,500", 3))
        assertEquals(62_500L, parseScaled("62,50000", 3))
    }

    @Test
    fun `more decimals than the scale allows is rejected, never rounded`() {
        assertNull(parseScaled("82,5555", 3))
        assertNull(parseScaled("1,5", 0))
    }

    @Test
    fun `negatives, text and blanks are not numbers`() {
        assertNull(parseScaled("-5", 3))
        assertNull(parseScaled("abc", 3))
        assertNull(parseScaled("", 3))
        assertNull(parseScaled("1,2,3", 3))
        assertNull(parseScaled("1e3", 3))
    }

    @Test
    fun `a value that does not fit in a long is rejected`() {
        assertNull(parseScaled("99999999999999999999", 3))
    }

    @Test
    fun `whole numbers accept only digits that fit an int`() {
        assertEquals(45, parseWholeNumber(" 45 "))
        assertNull(parseWholeNumber("4,5"))
        assertNull(parseWholeNumber("-3"))
        assertNull(parseWholeNumber(""))
        assertNull(parseWholeNumber("99999999999"))
    }

    @Test
    fun `editable text drops useless zeros`() {
        assertEquals("82.5", 82_500L.toEditableText(3))
        assertEquals("82", 82_000L.toEditableText(3))
        assertEquals("0.5", 500L.toEditableText(3))
    }

    @Test
    fun `sanitizing keeps digits and one separator with a decimal limit`() {
        assertEquals("82,5", sanitizeDecimalInput("82,5", 3))
        assertEquals("82.512", sanitizeDecimalInput("8a2.5-1 2x", 3))
        assertEquals("82.512", sanitizeDecimalInput("82.5123", 3))
        assertEquals("825", sanitizeDecimalInput("82,5", 0))
        // Un segundo separador se ignora y los dígitos siguientes siguen contando (igual que en Finanzas).
        assertEquals("1.57", sanitizeDecimalInput("1.5.7", 3))
    }
}
