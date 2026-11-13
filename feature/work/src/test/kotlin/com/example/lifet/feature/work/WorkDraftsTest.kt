package com.example.lifet.feature.work

import com.example.lifet.core.model.work.ShiftIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WorkDraftsTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 3, 12)
    private val now: Instant = LocalDateTime.parse("2026-03-12T20:00").atZone(zone).toInstant()

    private fun at(time: String): Instant = LocalDateTime.parse(time).atZone(zone).toInstant()

    @Test
    fun `the blank draft is a regular nine to five day and valid`() {
        val draft = blankShiftDraft("a", today)

        assertEquals(8 * 3_600L, draft.toShift(zone).netSecondsAt(now))
        assertTrue(draft.issues(zone, now).isEmpty())
        assertFalse(draft.endsNextDay)
    }

    @Test
    fun `an exit at or before the entry means the next day, so a night shift is valid`() {
        val night = blankShiftDraft("a", today.minusDays(1), startMinute = 22 * 60, endMinute = 6 * 60)

        assertTrue(night.endsNextDay)
        assertEquals(8 * 3_600L, night.toShift(zone).netSecondsAt(now))
        assertTrue(night.issues(zone, now).isEmpty())
    }

    @Test
    fun `the same hour for entry and exit is a 24 hour shift, which is the limit`() {
        val full = blankShiftDraft("a", today.minusDays(1), startMinute = 9 * 60, endMinute = 9 * 60)

        assertEquals(24 * 3_600L, full.toShift(zone).netSecondsAt(now))
        assertTrue(full.issues(zone, now).isEmpty())
    }

    @Test
    fun `the night of the spring DST change lasts seven hours in real time`() {
        val dst = blankShiftDraft("a", LocalDate.of(2026, 3, 28), startMinute = 22 * 60, endMinute = 6 * 60)

        assertEquals(7 * 3_600L, dst.toShift(zone).netSecondsAt(now))
    }

    @Test
    fun `a shift in the future is reported`() {
        val future = blankShiftDraft("a", today.plusDays(1))

        assertTrue(ShiftIssue.START_IN_FUTURE in future.issues(zone, now))
    }

    @Test
    fun `breaks are placed after the entry and overlapping ones are reported`() {
        val ok = blankShiftDraft("a", today).copy(breaks = listOf(BreakDraft(13 * 60, 13 * 60 + 30)))
        val overlap =
            blankShiftDraft("a", today).copy(
                breaks = listOf(BreakDraft(12 * 60, 13 * 60), BreakDraft(12 * 60 + 30, 14 * 60)),
            )

        assertEquals(30 * 60L, ok.toShift(zone).breakSecondsAt(now))
        assertEquals(setOf(ShiftIssue.BREAKS_OVERLAP), overlap.issues(zone, now))
    }

    @Test
    fun `a break outside the working hours is reported`() {
        val early = blankShiftDraft("a", today).copy(breaks = listOf(BreakDraft(8 * 60, 8 * 60 + 30)))

        // Antes de las 9:00 la pausa cae al día siguiente a la entrada: fuera de la jornada de 9 a 17.
        assertTrue(ShiftIssue.BREAK_OUTSIDE_SHIFT in early.issues(zone, now))
    }

    @Test
    fun `an existing shift round trips through its draft`() {
        val original =
            blankShiftDraft("a", today).copy(
                projectId = "p",
                note = "Cierre 🚀",
                breaks =
                    listOf(
                        BreakDraft(13 * 60, 13 * 60 + 45),
                    ),
            )

        val rebuilt = original.toShift(zone).toDraft()

        assertEquals(original, rebuilt)
    }

    @Test
    fun `an open shift draft keeps no end`() {
        val open = blankShiftDraft("a", today).copy(hasEnd = false)

        assertNull(open.toShift(zone).end)
        assertFalse(open.endsNextDay)
    }

    @Test
    fun `next occurrence picks today or tomorrow`() {
        val start = at("2026-03-12T10:00")

        assertEquals(at("2026-03-12T12:00"), nextOccurrence(start, 12 * 60, zone))
        assertEquals(at("2026-03-13T08:00"), nextOccurrence(start, 8 * 60, zone))
    }

    @Test
    fun `hours are parsed exactly and never rounded`() {
        assertEquals(2_400, parseHoursToMinutes("40"))
        assertEquals(2_250, parseHoursToMinutes("37,5"))
        assertEquals(2_250, parseHoursToMinutes(" 37.5 "))
        assertNull(parseHoursToMinutes("37,55"))
        assertNull(parseHoursToMinutes(""))
        assertNull(parseHoursToMinutes("abc"))
        assertNull(parseHoursToMinutes("0"))
        assertNull(parseHoursToMinutes("-5"))
        assertNull(parseHoursToMinutes("169"))
        assertNull(parseHoursToMinutes("9".repeat(40)))
    }

    @Test
    fun `hours text drops useless zeros`() {
        assertEquals("40", minutesToHoursText(2_400))
        assertEquals("37.5", minutesToHoursText(2_250))
    }

    @Test
    fun `settings draft needs a valid target and at least one day`() {
        assertTrue(SettingsDraft("40", setOf(DayOfWeek.MONDAY)).isValid)
        assertFalse(SettingsDraft("40", emptySet()).isValid)
        assertFalse(SettingsDraft("", setOf(DayOfWeek.MONDAY)).isValid)
        assertEquals(2_250, SettingsDraft("37,5", setOf(DayOfWeek.MONDAY)).toSettings()?.weeklyTargetMinutes)
    }

    @Test
    fun `a project draft needs a name`() {
        assertFalse(ProjectDraft(null, "   ").isValid)
        assertTrue(ProjectDraft(null, "Web").isValid)
    }
}
