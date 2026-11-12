package com.example.lifet.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateRangeTest {
    @Test
    fun `week starts on monday`() {
        val range = DateRange.containing(DateRangeUnit.WEEK, LocalDate.parse("2026-03-12"))

        assertEquals(LocalDate.parse("2026-03-09"), range.start)
        assertEquals(LocalDate.parse("2026-03-15"), range.endInclusive)
        assertEquals(7, range.days.size)
    }

    @Test
    fun `month handles february in leap and non leap years`() {
        val leap = DateRange.containing(DateRangeUnit.MONTH, LocalDate.parse("2028-02-10"))
        val normal = DateRange.containing(DateRangeUnit.MONTH, LocalDate.parse("2027-02-10"))

        assertEquals(29, leap.days.size)
        assertEquals(28, normal.days.size)
    }

    @Test
    fun `shift crosses year boundary`() {
        val december = DateRange.containing(DateRangeUnit.MONTH, LocalDate.parse("2026-12-20"))

        assertEquals(LocalDate.parse("2027-01-01"), december.shift(1).start)
        assertEquals(LocalDate.parse("2026-11-01"), december.shift(-1).start)
    }

    @Test
    fun `end is exclusive`() {
        val day = DateRange.containing(DateRangeUnit.DAY, LocalDate.parse("2026-03-10"))

        assertTrue(LocalDate.parse("2026-03-10") in day)
        assertFalse(LocalDate.parse("2026-03-11") in day)
    }
}
