package com.example.lifet.core.database.work

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.ProjectColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class WorkDaoTest {
    private lateinit var database: LifeTrackerDatabase
    private lateinit var dao: WorkDao
    private val zone = ZoneId.of("Europe/Madrid")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
        dao = database.workDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun shift(
        id: String,
        start: String,
        end: String? = null,
        projectId: String? = null,
    ) = WorkShiftEntity(id, Instant.parse(start), zone, end?.let(Instant::parse), projectId, "")

    private fun pause(
        shiftId: String,
        start: String,
        end: String? = null,
    ) = WorkBreakEntity(shiftId = shiftId, start = Instant.parse(start), end = end?.let(Instant::parse))

    @Test
    fun `shifts come back most recent first with their zone and open end`() =
        runTest {
            dao.saveShift(shift("a", "2026-03-09T08:00:00Z", "2026-03-09T16:00:00Z"), emptyList())
            dao.saveShift(shift("b", "2026-03-10T08:00:00Z"), emptyList())

            val shifts = dao.observeShifts().first()

            assertEquals(listOf("b", "a"), shifts.map { it.id })
            assertNull(shifts.first().end)
            assertEquals(zone, shifts.first().zoneId)
        }

    @Test
    fun `saving a shift again replaces its breaks instead of piling them up`() =
        runTest {
            dao.saveShift(
                shift("a", "2026-03-10T08:00:00Z"),
                listOf(pause("a", "2026-03-10T10:00:00Z", "2026-03-10T10:15:00Z"), pause("a", "2026-03-10T12:00:00Z")),
            )
            dao.saveShift(
                shift("a", "2026-03-10T08:00:00Z", "2026-03-10T16:00:00Z"),
                listOf(pause("a", "2026-03-10T10:00:00Z", "2026-03-10T10:15:00Z")),
            )

            val breaks = dao.observeBreaks().first()

            assertEquals(1, breaks.size)
            assertEquals(Instant.parse("2026-03-10T10:15:00Z"), breaks.single().end)
        }

    @Test
    fun `deleting a shift deletes its breaks`() =
        runTest {
            dao.saveShift(shift("a", "2026-03-10T08:00:00Z"), listOf(pause("a", "2026-03-10T10:00:00Z")))

            dao.deleteShift("a")

            assertTrue(dao.observeShifts().first().isEmpty())
            assertTrue(dao.observeBreaks().first().isEmpty())
        }

    @Test
    fun `deleting a project leaves its shifts untouched so undo restores everything`() =
        runTest {
            dao.upsertProject(WorkProjectEntity("p", "Web", ProjectColor.TEAL))
            dao.saveShift(shift("a", "2026-03-10T08:00:00Z", "2026-03-10T16:00:00Z", projectId = "p"), emptyList())

            dao.deleteProject("p")

            assertTrue(dao.observeProjects().first().isEmpty())
            assertEquals(
                "p",
                dao
                    .observeShifts()
                    .first()
                    .single()
                    .projectId,
            )
        }

    @Test
    fun `projects are listed alphabetically ignoring case`() =
        runTest {
            dao.upsertProject(WorkProjectEntity("1", "zeta", ProjectColor.RED))
            dao.upsertProject(WorkProjectEntity("2", "Alfa", ProjectColor.BLUE))
            dao.upsertProject(WorkProjectEntity("3", "beta", ProjectColor.GREEN))

            assertEquals(listOf("Alfa", "beta", "zeta"), dao.observeProjects().first().map { it.name })
        }

    @Test
    fun `one note per day and one settings row`() =
        runTest {
            val day = LocalDate.of(2026, 3, 10)
            dao.upsertNote(WorkDayNoteEntity(day, "primera"))
            dao.upsertNote(WorkDayNoteEntity(day, "segunda"))
            dao.upsertSettings(WorkSettingsEntity(weeklyTargetMinutes = 2_400, workingDaysMask = 31))
            dao.upsertSettings(WorkSettingsEntity(weeklyTargetMinutes = 1_200, workingDaysMask = 3))

            assertEquals(listOf("segunda"), dao.observeNotes().first().map { it.text })
            assertEquals(1_200, dao.observeSettings().first()?.weeklyTargetMinutes)

            dao.deleteNote(day)
            assertTrue(dao.observeNotes().first().isEmpty())
        }

    @Test
    fun `settings are null until saved`() =
        runTest {
            assertNull(dao.observeSettings().first())
        }

    @Test
    fun `a focus session keeps its start instant and finishes in place`() =
        runTest {
            val start = Instant.parse("2026-03-10T09:00:00Z")
            val running = WorkFocusEntity("f", FocusKind.FOCUS, start, zone, 1_500, null, false, null, "Informe")
            dao.upsertFocusSession(running)

            dao.upsertFocusSession(running.copy(end = start.plusSeconds(1_500), completed = true))

            val saved = dao.observeFocusSessions().first().single()
            assertEquals(start, saved.start)
            assertTrue(saved.completed)
            assertEquals(FocusKind.FOCUS, saved.kind)
        }

    @Test
    fun `replaceAll swaps the whole content and clears the breaks of removed shifts`() =
        runTest {
            dao.saveShift(shift("old", "2026-03-10T08:00:00Z"), listOf(pause("old", "2026-03-10T10:00:00Z")))
            dao.upsertProject(WorkProjectEntity("p-old", "Viejo", ProjectColor.GREY))

            dao.replaceAll(
                shifts = listOf(shift("new", "2026-03-11T08:00:00Z", "2026-03-11T16:00:00Z")),
                breaks = listOf(pause("new", "2026-03-11T12:00:00Z", "2026-03-11T12:30:00Z")),
                projects = listOf(WorkProjectEntity("p-new", "Nuevo", ProjectColor.BLUE)),
                focusSessions = emptyList(),
                notes = listOf(WorkDayNoteEntity(LocalDate.of(2026, 3, 11), "nota")),
                settings = WorkSettingsEntity(weeklyTargetMinutes = 1_800, workingDaysMask = 7),
            )

            assertEquals(listOf("new"), dao.observeShifts().first().map { it.id })
            assertEquals(listOf("new"), dao.observeBreaks().first().map { it.shiftId })
            assertEquals(listOf("Nuevo"), dao.observeProjects().first().map { it.name })
            assertEquals(1_800, dao.observeSettings().first()?.weeklyTargetMinutes)
        }
}
