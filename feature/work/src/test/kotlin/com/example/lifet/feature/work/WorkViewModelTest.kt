package com.example.lifet.feature.work

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.lifet.core.data.work.FocusTimerAlarm
import com.example.lifet.core.domain.work.ManageFocusTimer
import com.example.lifet.core.domain.work.ManageWorkClock
import com.example.lifet.core.domain.work.ManageWorkData
import com.example.lifet.core.domain.work.ObserveWorkOverview
import com.example.lifet.core.domain.work.ObserveWorkWeek
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.work.InMemoryWorkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class WorkViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone: ZoneId = TestClock.DEFAULT_ZONE
    private val today = LocalDate.of(2026, 3, 12)

    private fun at(time: String): Instant = LocalDateTime.parse(time).atZone(zone).toInstant()

    private fun clockAt(time: String): Clock = Clock.fixed(at(time), zone)

    /** Reloj que avanza a mano: un fichaje y su salida no pueden ocurrir en el mismo instante. */
    private class SteppingClock(
        private var current: Instant,
        private val zoneId: ZoneId,
    ) : Clock() {
        override fun getZone(): ZoneId = zoneId

        override fun withZone(zone: ZoneId): Clock = SteppingClock(current, zone)

        override fun instant(): Instant = current

        fun advanceMinutes(minutes: Long) {
            current = current.plusSeconds(minutes * 60)
        }
    }

    /** Fin de la jornada de hoy: así las jornadas manuales de 9 a 17 quedan en el pasado. */
    private val endOfDay = clockAt("2026-03-12T20:00")

    private class SilentAlarm : FocusTimerAlarm {
        override fun schedule(session: FocusSession) = Unit

        override fun cancel(sessionId: String) = Unit
    }

    private fun viewModel(
        repository: InMemoryWorkRepository,
        clock: Clock = clockAt("2026-03-12T12:00"),
        savedState: SavedStateHandle = SavedStateHandle(),
    ): WorkViewModel {
        val manageClock = ManageWorkClock(repository, clock)
        val manageFocus = ManageFocusTimer(repository, SilentAlarm(), clock)
        val manageData = ManageWorkData(repository)
        return WorkViewModel(
            observeOverview = ObserveWorkOverview(repository, clock),
            observeWeek = ObserveWorkWeek(repository, clock),
            clockActions = manageClock,
            focusActions = manageFocus,
            writer = WorkWriter(manageClock, manageData),
            clock = clock,
            savedState = savedState,
        )
    }

    private fun shift(
        id: String,
        start: String,
        end: String?,
    ) = WorkShift(id, at(start), zone, end?.let(::at))

    @Test
    fun `emits Loading and then Empty when nothing was recorded`() =
        runTest {
            val viewModel = viewModel(InMemoryWorkRepository())

            viewModel.uiState.test {
                assertEquals(WorkUiState.Loading, awaitItem())
                val empty = awaitItem() as WorkUiState.Empty
                assertEquals(40 * 3_600L, empty.overview.summary.weeklyTargetSeconds)
            }
        }

    @Test
    fun `emits Success with today's hours when there is a shift`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-12T08:00", "2026-03-12T11:00"))))

            viewModel(repository).uiState.test {
                assertEquals(WorkUiState.Loading, awaitItem())
                val success = awaitItem() as WorkUiState.Success
                assertEquals(3 * 3_600L, success.overview.summary.todaySeconds)
                assertEquals(7, success.week.days.size)
            }
        }

    @Test
    fun `emits Error when the data source fails and recovers on retry`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-12T08:00", "2026-03-12T11:00"))))
                    .apply { failObserve = true }
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(WorkUiState.Loading, awaitItem())
                assertEquals(R.string.work_error_load, (awaitItem() as WorkUiState.Error).messageRes)

                repository.failObserve = false
                viewModel.onAction(WorkAction.Retry)

                assertTrue(awaitItem() is WorkUiState.Success)
            }
        }

    @Test
    fun `clock in, break, resume and out work through the actions`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val clock = SteppingClock(at("2026-03-12T09:00"), zone)
            val viewModel = viewModel(repository, clock)

            viewModel.onAction(WorkAction.ClockIn(null))
            advanceUntilIdle()
            assertTrue(
                repository.current.shifts
                    .single()
                    .isOpen,
            )

            clock.advanceMinutes(120)
            viewModel.onAction(WorkAction.StartBreak)
            advanceUntilIdle()
            assertTrue(
                repository.current.shifts
                    .single()
                    .isOnBreak,
            )

            clock.advanceMinutes(30)
            viewModel.onAction(WorkAction.EndBreak)
            advanceUntilIdle()
            assertNull(
                repository.current.shifts
                    .single()
                    .openBreak,
            )

            clock.advanceMinutes(240)
            viewModel.onAction(WorkAction.ClockOut)
            advanceUntilIdle()
            val closed = repository.current.shifts.single()
            assertNotNull(closed.end)
            // 09:00 a 15:30 son 6,5 h; la pausa de 30 min se descuenta.
            assertEquals(6 * 3_600L, closed.netSecondsAt(clock.instant()))
        }

    @Test
    fun `clocking out with nothing in progress explains why`() =
        runTest {
            val viewModel = viewModel(InMemoryWorkRepository())

            viewModel.effects.test {
                viewModel.onAction(WorkAction.ClockOut)
                assertEquals(WorkEffect.Message(R.string.work_issue_not_clocked_in), awaitItem())
            }
        }

    @Test
    fun `opening a shift stamps today, the zone and now from the injected clock`() =
        runTest {
            val viewModel = viewModel(InMemoryWorkRepository())

            viewModel.onAction(WorkAction.OpenShift())

            val editor = viewModel.editor.value as WorkEditor.EditShift
            assertEquals(today, editor.today)
            assertEquals(zone, editor.zone)
            assertEquals(at("2026-03-12T12:00"), editor.now)
            assertNull(editor.existing)
            assertTrue(editor.newId.isNotBlank())
        }

    @Test
    fun `saving a valid manual shift stores it, closes the sheet and confirms`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val viewModel = viewModel(repository, endOfDay)
            viewModel.onAction(WorkAction.OpenShift())
            val draft = blankShiftDraft("new", today)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Save(WorkForm.Shift(draft, null, zone)))
                assertEquals(WorkEffect.Saved, awaitItem())
            }

            val saved = repository.current.shifts.single()
            assertEquals(8 * 3_600L, saved.netSecondsAt(at("2026-03-12T12:00")))
            assertEquals(zone, saved.zone)
            assertNull(viewModel.editor.value)
        }

    @Test
    fun `a shift with overlapping breaks is rejected with its own message and nothing is written`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val viewModel = viewModel(repository, endOfDay)
            val draft =
                blankShiftDraft("new", today).copy(
                    breaks = listOf(BreakDraft(12 * 60, 13 * 60), BreakDraft(12 * 60 + 30, 13 * 60 + 30)),
                )
            viewModel.onAction(WorkAction.OpenShift())

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Save(WorkForm.Shift(draft, null, zone)))
                assertEquals(WorkEffect.Message(R.string.work_issue_breaks_overlap), awaitItem())
            }

            assertTrue(repository.current.shifts.isEmpty())
            assertNotNull(viewModel.editor.value)
        }

    @Test
    fun `a shift that overlaps another one reports it`() =
        runTest {
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-12T08:00", "2026-03-12T10:00"))))
            val viewModel = viewModel(repository, endOfDay)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Save(WorkForm.Shift(blankShiftDraft("b", today), null, zone)))
                assertEquals(WorkEffect.Message(R.string.work_issue_overlaps_shift), awaitItem())
            }
        }

    @Test
    fun `a storage failure while saving shows the save error`() =
        runTest {
            val repository = InMemoryWorkRepository().apply { failWrites = true }
            val viewModel = viewModel(repository, endOfDay)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Save(WorkForm.Shift(blankShiftDraft("a", today), null, zone)))
                assertEquals(WorkEffect.Message(R.string.work_error_save), awaitItem())
            }
        }

    @Test
    fun `a forgotten shift can be closed afterwards from the sheet`() =
        runTest {
            val forgotten = shift("a", "2026-03-09T09:00", null)
            val repository = InMemoryWorkRepository(WorkDataSet(shifts = listOf(forgotten)))
            val viewModel = viewModel(repository)

            viewModel.onAction(WorkAction.OpenClose(forgotten))
            assertTrue(viewModel.editor.value is WorkEditor.CloseShift)
            viewModel.effects.test {
                viewModel.onAction(
                    WorkAction.Save(WorkForm.Close(forgotten, LocalDate.of(2026, 3, 9), 18 * 60, zone)),
                )
                assertEquals(WorkEffect.Saved, awaitItem())
            }

            assertEquals(
                at("2026-03-09T18:00"),
                repository.current.shifts
                    .single()
                    .end,
            )
        }

    @Test
    fun `closing a forgotten shift before its entry is rejected`() =
        runTest {
            val forgotten = shift("a", "2026-03-09T09:00", null)
            val repository = InMemoryWorkRepository(WorkDataSet(shifts = listOf(forgotten)))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(
                    WorkAction.Save(WorkForm.Close(forgotten, LocalDate.of(2026, 3, 9), 8 * 60, zone)),
                )
                assertEquals(WorkEffect.Message(R.string.work_issue_end_before_start), awaitItem())
            }
        }

    @Test
    fun `deleting a shift offers undo and undo restores it`() =
        runTest {
            val original = shift("a", "2026-03-12T08:00", "2026-03-12T10:00")
            val repository = InMemoryWorkRepository(WorkDataSet(shifts = listOf(original)))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Delete(WorkItem.Shift(original)))
                val deleted = awaitItem() as WorkEffect.Deleted
                assertEquals(R.string.work_deleted_shift, deleted.messageRes)
                assertTrue(repository.current.shifts.isEmpty())

                viewModel.onAction(WorkAction.Undo(deleted.item))
                advanceUntilIdle()
            }

            assertEquals(listOf(original), repository.current.shifts)
        }

    @Test
    fun `deleting a project keeps its shifts and undo brings it back`() =
        runTest {
            val project = WorkProject("p", "Web", ProjectColor.BLUE)
            val repository =
                InMemoryWorkRepository(
                    WorkDataSet(
                        shifts = listOf(shift("a", "2026-03-12T08:00", "2026-03-12T10:00").copy(projectId = "p")),
                        projects = listOf(project),
                    ),
                )
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Delete(WorkItem.Project(project)))
                val deleted = awaitItem() as WorkEffect.Deleted
                assertTrue(repository.current.projects.isEmpty())
                assertEquals(1, repository.current.shifts.size)

                viewModel.onAction(WorkAction.Undo(deleted.item))
                advanceUntilIdle()
            }

            assertEquals(listOf(project), repository.current.projects)
        }

    @Test
    fun `a project without a name is rejected`() =
        runTest {
            val viewModel = viewModel(InMemoryWorkRepository())

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Save(WorkForm.Project(ProjectDraft(id = null, name = "  "))))
                assertEquals(WorkEffect.Message(R.string.work_issue_name_missing), awaitItem())
            }
        }

    @Test
    fun `the day note is saved and a blank one removes it`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Save(WorkForm.Note(today, "Reunión larga 🗓️")))
                assertEquals(WorkEffect.Saved, awaitItem())
                assertEquals(
                    "Reunión larga 🗓️",
                    repository.current.notes
                        .single()
                        .text,
                )

                viewModel.onAction(WorkAction.Save(WorkForm.Note(today, "")))
                assertEquals(WorkEffect.Saved, awaitItem())
            }
            assertTrue(repository.current.notes.isEmpty())
        }

    @Test
    fun `settings with an impossible target are rejected and valid ones saved`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(
                    WorkAction.Save(WorkForm.Settings(SettingsDraft("abc", setOf(java.time.DayOfWeek.MONDAY)))),
                )
                assertEquals(WorkEffect.Message(R.string.work_issue_target_invalid), awaitItem())

                viewModel.onAction(
                    WorkAction.Save(WorkForm.Settings(SettingsDraft("37,5", setOf(java.time.DayOfWeek.MONDAY)))),
                )
                assertEquals(WorkEffect.Saved, awaitItem())
            }
            assertEquals(2_250, repository.current.settings.weeklyTargetMinutes)
        }

    @Test
    fun `the focus timer starts, stops and rejects a second one`() =
        runTest {
            val repository = InMemoryWorkRepository()
            val viewModel = viewModel(repository, clockAt("2026-03-12T10:00"))

            viewModel.onAction(WorkAction.StartFocus(FocusKind.FOCUS, 25, null, "Informe"))
            advanceUntilIdle()
            assertEquals(
                25 * 60L,
                repository.current.focusSessions
                    .single()
                    .plannedSeconds,
            )

            viewModel.effects.test {
                viewModel.onAction(WorkAction.StartFocus(FocusKind.FOCUS, 25, null, ""))
                assertEquals(WorkEffect.Message(R.string.work_issue_timer_running), awaitItem())
            }
        }

    @Test
    fun `an expired focus session is closed by the screen and announced`() =
        runTest {
            val repository = InMemoryWorkRepository()
            ManageFocusTimer(repository, SilentAlarm(), clockAt("2026-03-12T09:00")).start(FocusKind.FOCUS, 25)
            val viewModel = viewModel(repository, clockAt("2026-03-12T12:00"))

            viewModel.effects.test {
                viewModel.onAction(WorkAction.FinishFocus)
                assertEquals(WorkEffect.FocusFinished(FocusKind.FOCUS), awaitItem())
            }
            assertTrue(
                repository.current.focusSessions
                    .single()
                    .completed,
            )
        }

    @Test
    fun `the week can be moved back but never into the future and survives a saved state`() =
        runTest {
            val saved = SavedStateHandle()
            val repository =
                InMemoryWorkRepository(WorkDataSet(shifts = listOf(shift("a", "2026-03-04T08:00", "2026-03-04T10:00"))))
            val viewModel = viewModel(repository, savedState = saved)

            viewModel.uiState.test {
                awaitItem()
                assertTrue((awaitItem() as WorkUiState.Success).week.isCurrentWeek)

                viewModel.onAction(WorkAction.MoveWeek(-1))
                val previous = awaitItem() as WorkUiState.Success
                assertEquals(LocalDate.of(2026, 3, 2), previous.week.week.start)
                assertEquals(2 * 3_600L, previous.week.workedSeconds)

                viewModel.onAction(WorkAction.MoveWeek(1))
                assertTrue((awaitItem() as WorkUiState.Success).week.isCurrentWeek)
                viewModel.onAction(WorkAction.MoveWeek(1))
                expectNoEvents()
            }

            // Un ViewModel nuevo con el mismo estado guardado recupera la semana elegida.
            saved[KEY_WEEK] = -2
            val restored = viewModel(repository, savedState = saved)
            restored.uiState.test {
                awaitItem()
                assertEquals(LocalDate.of(2026, 2, 23), (awaitItem() as WorkUiState.Success).week.week.start)
            }
        }

    @Test
    fun `tapping a day in the week opens a new shift on that day, never in the future`() =
        runTest {
            val viewModel = viewModel(InMemoryWorkRepository(), endOfDay)

            viewModel.onAction(WorkAction.OpenShift(date = LocalDate.of(2026, 3, 10)))
            assertEquals(LocalDate.of(2026, 3, 10), (viewModel.editor.value as WorkEditor.EditShift).startDate)

            viewModel.onAction(WorkAction.OpenShift(date = LocalDate.of(2026, 3, 20)))
            assertEquals(today, (viewModel.editor.value as WorkEditor.EditShift).startDate)

            viewModel.onAction(WorkAction.OpenShift())
            assertEquals(today, (viewModel.editor.value as WorkEditor.EditShift).startDate)
        }

    @Test
    fun `undoing a delete that now collides with another shift is rejected and explained`() =
        runTest {
            val deleted = shift("a", "2026-03-12T08:00", "2026-03-12T12:00")
            val replacement = shift("b", "2026-03-12T09:00", "2026-03-12T11:00")
            val repository = InMemoryWorkRepository(WorkDataSet(shifts = listOf(replacement)))
            val viewModel = viewModel(repository, endOfDay)

            viewModel.effects.test {
                viewModel.onAction(WorkAction.Undo(WorkItem.Shift(deleted)))
                assertEquals(WorkEffect.Message(R.string.work_error_save), awaitItem())
            }
            assertEquals(listOf("b"), repository.current.shifts.map { it.id })
        }

    @Test
    fun `the settings and note editors open without needing an existing record`() =
        runTest {
            val viewModel = viewModel(InMemoryWorkRepository())

            viewModel.onAction(WorkAction.OpenSettings)
            assertEquals(WorkEditor.EditSettings, viewModel.editor.value)

            viewModel.onAction(WorkAction.OpenNote(today, "hola"))
            assertEquals(WorkEditor.EditNote(today, "hola"), viewModel.editor.value)

            viewModel.onAction(WorkAction.Dismiss)
            assertNull(viewModel.editor.value)
        }

    private companion object {
        const val KEY_WEEK = "weekOffset"
    }
}
