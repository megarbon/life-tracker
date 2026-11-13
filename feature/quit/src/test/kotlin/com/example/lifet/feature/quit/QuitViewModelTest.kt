package com.example.lifet.feature.quit

import app.cash.turbine.test
import com.example.lifet.core.domain.quit.ObserveQuitOverview
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.quit.InMemoryQuitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class QuitViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = TestClock.fixed("2026-03-10T11:00:00Z")
    private val zone = clock.zone
    private val today = LocalDate.of(2026, 3, 10)

    private fun habit() =
        QuitHabit(
            id = "tobacco",
            kind = HabitKind.TOBACCO,
            name = "Tabaco",
            mode = HabitMode.QUIT,
            startedAt =
                today
                    .minusDays(30)
                    .atTime(8, 0)
                    .atZone(zone)
                    .toInstant(),
            zone = zone,
            baselineUnits = 20,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 20,
            packPrice = Money(540, CurrencyCode.EUR),
        )

    private fun viewModel(repository: InMemoryQuitRepository): QuitViewModel =
        QuitViewModel(ObserveQuitOverview(repository, clock), QuitWriter(repository, clock), clock)

    private fun cravingForm(
        habitId: String = "tobacco",
        minute: Int = 9 * 60,
    ) = QuitForm.Craving(
        CravingDraft("new", habitId, today, minute, 4, CravingTrigger.COFFEE, CopingStrategy.BREATHING, ""),
        existing = null,
    )

    private fun consumptionForm(units: String = "2") =
        QuitForm.Consumption(
            ConsumptionDraft("slip", "tobacco", today, 9 * 60, units, CravingTrigger.SOCIAL, ""),
            existing = null,
        )

    @Test
    fun `emits Loading and then Empty when there is no habit`() =
        runTest {
            val viewModel = viewModel(InMemoryQuitRepository())

            viewModel.uiState.test {
                assertEquals(QuitUiState.Loading, awaitItem())
                assertEquals(QuitUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `emits Success with the habit progress`() =
        runTest {
            val viewModel = viewModel(InMemoryQuitRepository(QuitDataSet(habits = listOf(habit()))))

            viewModel.uiState.test {
                assertEquals(QuitUiState.Loading, awaitItem())
                val success = awaitItem() as QuitUiState.Success
                val progress = success.overview.habits.single()
                assertEquals("tobacco", success.overview.primaryId)
                // De las 08:00 del día 30 a las 12:00 de hoy hay 30 días y 4 horas.
                assertEquals(30 * 86_400L + 4 * 3_600L, progress.cleanSeconds)
            }
        }

    @Test
    fun `emits Error when loading fails and recovers on retry`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            repository.failObserve = true
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(QuitUiState.Loading, awaitItem())
                assertEquals(QuitUiState.Error(R.string.quit_error_load), awaitItem())

                repository.failObserve = false
                viewModel.onAction(QuitAction.Retry)

                assertTrue(awaitItem() is QuitUiState.Success)
            }
        }

    @Test
    fun `saving a craving closes the sheet, stores it and confirms kindly`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(QuitAction.OpenEvent("tobacco"))
                advanceUntilIdle()
                assertNotNull(viewModel.editor.value)

                viewModel.onAction(QuitAction.Save(cravingForm()))

                assertEquals(QuitEffect.Saved(R.string.quit_saved_craving), awaitItem())
                assertNull(viewModel.editor.value)
                assertEquals(1, repository.current.cravings.size)
                assertEquals(
                    CopingStrategy.BREATHING,
                    repository.current.cravings
                        .single()
                        .strategy,
                )
            }
        }

    @Test
    fun `a slip is saved without erasing the record and the message is reassuring`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Save(consumptionForm("3")))

                assertEquals(QuitEffect.Saved(R.string.quit_saved_consumption), awaitItem())
                assertEquals(
                    3,
                    repository.current.consumptions
                        .single()
                        .units,
                )
            }
            viewModel.uiState.test {
                skipItems(1)
                val progress = (awaitItem() as QuitUiState.Success).overview.habits.single()
                assertEquals(1, progress.slips)
                assertTrue(progress.bestStreakSeconds >= progress.cleanSeconds)
            }
        }

    @Test
    fun `an invalid form reports an error and saves nothing`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Save(consumptionForm(units = "")))

                assertEquals(QuitEffect.Message(R.string.quit_error_invalid), awaitItem())
                assertTrue(repository.current.consumptions.isEmpty())
            }
        }

    @Test
    fun `a record before the habit started is rejected`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            val viewModel = viewModel(repository)
            val early =
                QuitForm.Craving(
                    CravingDraft("c", "tobacco", today.minusDays(40), 9 * 60, 3, CravingTrigger.OTHER, null, ""),
                    existing = null,
                )

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Save(early))

                assertEquals(QuitEffect.Message(R.string.quit_error_invalid), awaitItem())
                assertTrue(repository.current.cravings.isEmpty())
            }
        }

    @Test
    fun `a write failure is reported with a humane message`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            repository.failWrites = true
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Save(cravingForm()))

                assertEquals(QuitEffect.Message(R.string.quit_error_save), awaitItem())
            }
        }

    @Test
    fun `saving to an unknown habit fails instead of storing an orphan`() =
        runTest {
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit())))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Save(cravingForm(habitId = "missing")))

                assertEquals(QuitEffect.Message(R.string.quit_error_save), awaitItem())
                assertTrue(repository.current.cravings.isEmpty())
            }
        }

    @Test
    fun `deleting a slip offers undo and undo brings it back`() =
        runTest {
            val slip = QuitConsumption("s", "tobacco", today.atTime(9, 0).atZone(zone).toInstant(), zone, 2)
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit()), consumptions = listOf(slip)))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Delete(QuitItem.Consumption(slip)))
                val deleted = awaitItem() as QuitEffect.Deleted
                assertEquals(R.string.quit_deleted_consumption, deleted.messageRes)
                assertTrue(repository.current.consumptions.isEmpty())

                viewModel.onAction(QuitAction.Undo(deleted.item))
                advanceUntilIdle()

                assertEquals(listOf(slip), repository.current.consumptions)
            }
        }

    @Test
    fun `deleting a habit removes its history and undo restores everything`() =
        runTest {
            val craving =
                QuitCraving("c", "tobacco", today.atTime(9, 0).atZone(zone).toInstant(), zone, 3, CravingTrigger.COFFEE)
            val slip = QuitConsumption("s", "tobacco", today.atTime(10, 0).atZone(zone).toInstant(), zone, 1)
            val data = QuitDataSet(habits = listOf(habit()), cravings = listOf(craving), consumptions = listOf(slip))
            val repository = InMemoryQuitRepository(data)
            val viewModel = viewModel(repository)
            val item = QuitItem.Habit(habit(), listOf(craving), listOf(slip))

            viewModel.effects.test {
                viewModel.onAction(QuitAction.Delete(item))
                val deleted = awaitItem() as QuitEffect.Deleted
                assertEquals(QuitDataSet(), repository.current)

                viewModel.onAction(QuitAction.Undo(deleted.item))
                advanceUntilIdle()

                assertEquals(data, repository.current)
            }
        }

    @Test
    fun `opening a sheet stamps the injected clock and a stable id`() =
        runTest {
            val viewModel = viewModel(InMemoryQuitRepository(QuitDataSet(habits = listOf(habit()))))

            viewModel.onAction(QuitAction.OpenEvent("tobacco", asConsumption = true))
            val editor = viewModel.editor.value as QuitEditor.Event

            assertEquals(today, editor.today)
            assertEquals(zone, editor.zone)
            assertEquals(clock.instant(), editor.now)
            assertEquals(12 * 60, editor.nowMinute)
            assertTrue(editor.asConsumption)
            assertTrue(editor.newId.isNotBlank())
        }

    @Test
    fun `the sos sheet can be opened and dismissed`() =
        runTest {
            val viewModel = viewModel(InMemoryQuitRepository(QuitDataSet(habits = listOf(habit()))))

            viewModel.onAction(QuitAction.OpenSos("tobacco"))
            assertEquals(QuitEditor.Sos("tobacco"), viewModel.editor.value)

            viewModel.onAction(QuitAction.Dismiss)
            assertNull(viewModel.editor.value)
        }
}
