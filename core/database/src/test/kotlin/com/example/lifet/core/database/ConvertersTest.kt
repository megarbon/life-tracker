package com.example.lifet.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `instant survives a round trip with millisecond precision`() {
        val instant = Instant.parse("2026-03-10T09:15:30.123Z")

        assertEquals(instant, converters.longToInstant(converters.instantToLong(instant)))
    }

    @Test
    fun `zone id survives a round trip`() {
        val zone = ZoneId.of("Europe/Madrid")

        assertEquals(zone, converters.stringToZoneId(converters.zoneIdToString(zone)))
    }

    @Test
    fun `local date survives a round trip`() {
        val date = LocalDate.parse("2026-12-31")

        assertEquals(date, converters.longToLocalDate(converters.localDateToLong(date)))
    }

    @Test
    fun `null values stay null`() {
        assertNull(converters.instantToLong(null))
        assertNull(converters.longToInstant(null))
        assertNull(converters.zoneIdToString(null))
        assertNull(converters.stringToZoneId(null))
    }
}
