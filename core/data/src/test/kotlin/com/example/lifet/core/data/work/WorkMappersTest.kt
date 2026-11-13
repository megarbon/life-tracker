package com.example.lifet.core.data.work

import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class WorkMappersTest {
    private val zone = ZoneId.of("Europe/Madrid")

    private fun shift(vararg breaks: WorkBreak) =
        WorkShift(
            id = "s",
            start = Instant.parse("2026-03-09T08:00:00Z"),
            zone = zone,
            end = Instant.parse("2026-03-09T16:00:00Z"),
            breaks = breaks.toList(),
            projectId = "p",
            note = "Cierre de sprint 🚀",
        )

    @Test
    fun `a shift survives the round trip through its entities`() {
        val original =
            shift(
                WorkBreak(Instant.parse("2026-03-09T10:00:00Z"), Instant.parse("2026-03-09T10:15:00Z")),
                WorkBreak(Instant.parse("2026-03-09T12:00:00Z"), null),
            ).copy(end = null)

        val rebuilt = original.toEntity().toDomain(original.toBreakEntities())

        assertEquals(original, rebuilt)
    }

    @Test
    fun `breaks are rebuilt in chronological order whatever order the rows come in`() {
        val original =
            shift(
                WorkBreak(Instant.parse("2026-03-09T10:00:00Z"), Instant.parse("2026-03-09T10:15:00Z")),
                WorkBreak(Instant.parse("2026-03-09T12:00:00Z"), Instant.parse("2026-03-09T12:30:00Z")),
            )

        val rebuilt = original.toEntity().toDomain(original.toBreakEntities().reversed())

        assertEquals(original, rebuilt)
    }

    @Test
    fun `a shift without breaks produces no break rows`() {
        assertTrue(shift().toBreakEntities().isEmpty())
    }

    @Test
    fun `projects and focus sessions round trip`() {
        val project = WorkProject("p", "Rediseño web 🎨", ProjectColor.PURPLE)
        val session =
            FocusSession(
                id = "f",
                kind = FocusKind.LONG_BREAK,
                start = Instant.parse("2026-03-09T09:00:00Z"),
                zone = zone,
                plannedSeconds = 900,
                end = Instant.parse("2026-03-09T09:10:00Z"),
                completed = false,
                projectId = "p",
                title = "Informe",
            )

        assertEquals(project, project.toEntity().toDomain())
        assertEquals(session, session.toEntity().toDomain())
    }

    @Test
    fun `working days are stored as a bit mask and read back`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)

        assertEquals(1 + 4 + 64, days.toMask())
        assertEquals(days, days.toMask().toWorkingDays())
        assertEquals(WorkSettings.DEFAULT_WORKING_DAYS, WorkSettings.DEFAULT_WORKING_DAYS.toMask().toWorkingDays())
        assertTrue(0.toWorkingDays().isEmpty())
    }

    @Test
    fun `settings round trip`() {
        val settings =
            WorkSettings(weeklyTargetMinutes = 1_800, workingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY))

        assertEquals(settings, settings.toEntity().toDomain())
    }
}
