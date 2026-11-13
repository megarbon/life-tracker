package com.example.lifet.core.model.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftValidatorTest {
    private val now = at("2026-03-12T12:00")

    private fun issues(
        shift: WorkShift,
        others: List<WorkShift> = emptyList(),
    ) = ShiftValidator.validate(shift, others, now)

    @Test
    fun `a normal shift with a break is valid`() {
        val shift =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:00",
                breaks = listOf(pause("2026-03-10T13:00", "2026-03-10T13:30")),
            )

        assertTrue(issues(shift).isEmpty())
    }

    @Test
    fun `an open shift with an open break is valid`() {
        val shift = shift("a", "2026-03-12T09:00", null, breaks = listOf(pause("2026-03-12T11:00", null)))

        assertTrue(issues(shift).isEmpty())
    }

    @Test
    fun `end before start is rejected`() {
        assertEquals(
            setOf(ShiftIssue.END_BEFORE_START),
            issues(shift("a", "2026-03-10T09:00", "2026-03-10T08:00")),
        )
        assertEquals(
            setOf(ShiftIssue.END_BEFORE_START),
            issues(shift("a", "2026-03-10T09:00", "2026-03-10T09:00")),
        )
    }

    @Test
    fun `a shift in the future is rejected`() {
        val future = issues(shift("a", "2026-03-13T09:00", "2026-03-13T17:00"))

        assertTrue(ShiftIssue.START_IN_FUTURE in future)
        assertTrue(ShiftIssue.END_IN_FUTURE in future)
    }

    @Test
    fun `more than 24 hours is rejected and exactly 24 is accepted`() {
        assertEquals(setOf(ShiftIssue.TOO_LONG), issues(shift("a", "2026-03-10T09:00", "2026-03-11T09:01")))
        assertTrue(issues(shift("a", "2026-03-10T09:00", "2026-03-11T09:00")).isEmpty())
    }

    @Test
    fun `overlapping breaks are rejected`() {
        val shift =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:00",
                breaks =
                    listOf(
                        pause("2026-03-10T12:00", "2026-03-10T13:00"),
                        pause("2026-03-10T12:30", "2026-03-10T13:30"),
                    ),
            )

        assertEquals(setOf(ShiftIssue.BREAKS_OVERLAP), issues(shift))
    }

    @Test
    fun `back to back breaks are fine`() {
        val shift =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:00",
                breaks =
                    listOf(
                        pause("2026-03-10T12:00", "2026-03-10T12:30"),
                        pause("2026-03-10T12:30", "2026-03-10T13:00"),
                    ),
            )

        assertTrue(issues(shift).isEmpty())
    }

    @Test
    fun `a break outside the shift is rejected`() {
        val before =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:00",
                breaks = listOf(pause("2026-03-10T08:00", "2026-03-10T08:30")),
            )
        val after =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:00",
                breaks = listOf(pause("2026-03-10T16:45", "2026-03-10T17:30")),
            )

        assertEquals(setOf(ShiftIssue.BREAK_OUTSIDE_SHIFT), issues(before))
        assertEquals(setOf(ShiftIssue.BREAK_OUTSIDE_SHIFT), issues(after))
    }

    @Test
    fun `a break that ends before it starts is rejected`() {
        val shift =
            shift(
                "a",
                "2026-03-10T09:00",
                "2026-03-10T17:00",
                breaks = listOf(pause("2026-03-10T13:00", "2026-03-10T12:00")),
            )

        assertTrue(ShiftIssue.BREAK_END_BEFORE_START in issues(shift))
    }

    @Test
    fun `an open break in a closed shift or two open breaks are rejected`() {
        val closed =
            shift("a", "2026-03-10T09:00", "2026-03-10T17:00", breaks = listOf(pause("2026-03-10T13:00", null)))
        val twoOpen =
            shift(
                "b",
                "2026-03-12T09:00",
                null,
                breaks = listOf(pause("2026-03-12T10:00", null), pause("2026-03-12T11:00", null)),
            )

        assertTrue(ShiftIssue.OPEN_BREAK_IN_CLOSED_SHIFT in issues(closed))
        assertTrue(ShiftIssue.MORE_THAN_ONE_OPEN_BREAK in issues(twoOpen))
    }

    @Test
    fun `too many breaks and a too long note are rejected`() {
        val first = at("2026-03-10T09:00")
        val breaks =
            (0 until WorkRules.MAX_BREAKS + 1).map {
                WorkBreak(first.plusSeconds(it * 600L), first.plusSeconds(it * 600L + 300L))
            }
        val many = shift("a", "2026-03-10T08:00", "2026-03-10T17:00").copy(breaks = breaks)
        val longNote =
            shift("b", "2026-03-10T08:00", "2026-03-10T17:00").copy(
                note =
                    "x".repeat(WorkRules.SHIFT_NOTE_MAX_LENGTH + 1),
            )

        assertTrue(ShiftIssue.TOO_MANY_BREAKS in issues(many))
        assertEquals(setOf(ShiftIssue.NOTE_TOO_LONG), issues(longNote))
    }

    @Test
    fun `two shifts cannot overlap but the same shift being edited is ignored`() {
        val existing = shift("a", "2026-03-10T09:00", "2026-03-10T13:00")

        assertEquals(
            setOf(ShiftIssue.OVERLAPS_OTHER_SHIFT),
            issues(shift("b", "2026-03-10T12:00", "2026-03-10T14:00"), listOf(existing)),
        )
        assertTrue(issues(shift("b", "2026-03-10T13:00", "2026-03-10T14:00"), listOf(existing)).isEmpty())
        assertTrue(issues(shift("a", "2026-03-10T09:30", "2026-03-10T13:00"), listOf(existing)).isEmpty())
    }

    @Test
    fun `a forgotten open shift does not block the days after it`() {
        val forgotten = shift("a", "2026-03-10T09:00", null)

        assertTrue(issues(shift("b", "2026-03-11T09:00", "2026-03-11T17:00"), listOf(forgotten)).isEmpty())
        assertEquals(
            setOf(ShiftIssue.OVERLAPS_OTHER_SHIFT),
            issues(shift("c", "2026-03-10T10:00", "2026-03-10T11:00"), listOf(forgotten)),
        )
    }

    @Test
    fun `closing a forgotten shift at an hour after the next day is rejected as overlap or too long`() {
        val closing = shift("a", "2026-03-10T09:00", "2026-03-12T09:00")

        assertTrue(ShiftIssue.TOO_LONG in issues(closing))
    }

    @Test
    fun `a shift across the DST change is valid`() {
        val night = shift("a", "2026-03-28T22:00", "2026-03-29T06:00")

        assertTrue(ShiftValidator.validate(night, emptyList(), at("2026-03-30T12:00")).isEmpty())
    }
}
