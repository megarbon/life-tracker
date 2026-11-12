package com.example.lifet.feature.fitness

import app.cash.turbine.test
import com.example.lifet.core.domain.fitness.ObserveFitnessOverview
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutType
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.fitness.InMemoryFitnessRepository
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
class FitnessViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = TestClock.fixed("2026-03-11T19:30:00Z")
    private val zone = clock.zone
    private val today = LocalDate.of(2026, 3, 11)

    private fun run(
        id: String,
        date: LocalDate = today,
        minutes: Long = 30,
    ) = Workout(
        id = id,
        type = WorkoutType.RUN,
        title = "Parque",
        start = date.atTime(8, 0).atZone(zone).toInstant(),
        zone = zone,
        durationSec = minutes * 60,
        distanceM = 5_000,
    )

    private fun viewModel(repository: InMemoryFitnessRepository): FitnessViewModel =
        FitnessViewModel(ObserveFitnessOverview(repository, clock), FitnessWriter(repository, clock), clock)

    private fun cardioDraft(duration: String = "40") =
        WorkoutDraft(
            id = "new",
            type = WorkoutType.RUN,
            title = "Intervalos",
            date = today,
            startMinute = 7 * 60,
            durationText = duration,
            distanceText = "8,5",
        )

    @Test
    fun `emits Loading and then Empty when nothing was recorded`() =
        runTest {
            val viewModel = viewModel(InMemoryFitnessRepository())

            viewModel.uiState.test {
                assertEquals(FitnessUiState.Loading, awaitItem())
                val empty = awaitItem() as FitnessUiState.Empty
                assertEquals(150, empty.overview.summary.weeklyGoalMinutes)
            }
        }

    @Test
    fun `emits Success with the weekly minutes when there are workouts`() =
        runTest {
            val viewModel =
                viewModel(InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(run("a", minutes = 45)))))

            viewModel.uiState.test {
                assertEquals(FitnessUiState.Loading, awaitItem())
                val success = awaitItem() as FitnessUiState.Success
                assertEquals(45, success.overview.summary.weekActiveMinutes)
                assertTrue(success.overview.summary.workedOutToday)
            }
        }

    @Test
    fun `a weight alone is enough to leave the Empty state`() =
        runTest {
            val data = FitnessDataSet(weights = listOf(BodyWeight(today, 80_000)))
            val viewModel = viewModel(InMemoryFitnessRepository(data))

            viewModel.uiState.test {
                awaitItem()
                assertTrue(awaitItem() is FitnessUiState.Success)
            }
        }

    @Test
    fun `emits Error when the data source fails and recovers on retry`() =
        runTest {
            val repository =
                InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(run("a")))).apply {
                    failObserve =
                        true
                }
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(FitnessUiState.Loading, awaitItem())
                assertEquals(R.string.fitness_error_load, (awaitItem() as FitnessUiState.Error).messageRes)

                repository.failObserve = false
                viewModel.onAction(FitnessAction.Retry)

                assertTrue(awaitItem() is FitnessUiState.Success)
            }
        }

    @Test
    fun `opening a workout stamps today, the zone and now from the injected clock`() =
        runTest {
            val viewModel = viewModel(InMemoryFitnessRepository())

            viewModel.onAction(FitnessAction.OpenWorkout())

            val editor = viewModel.editor.value as FitnessEditor.EditWorkout
            assertEquals(today, editor.today)
            assertEquals(zone, editor.zone)
            assertEquals(20 * 60 + 30, editor.nowMinute)
            assertNull(editor.existing)
            assertTrue(editor.newId.isNotBlank())
        }

    @Test
    fun `saving a valid workout stores it, closes the sheet and confirms with haptics`() =
        runTest {
            val repository = InMemoryFitnessRepository()
            val viewModel = viewModel(repository)
            viewModel.onAction(FitnessAction.OpenWorkout())

            viewModel.effects.test {
                viewModel.onAction(FitnessAction.Save(FitnessForm.Workout(cardioDraft(), existing = null)))
                assertEquals(FitnessEffect.Saved, awaitItem())
            }

            val saved = repository.current.workouts.single()
            assertEquals(40 * 60L, saved.durationSec)
            assertEquals(8_500L, saved.distanceM)
            assertEquals(zone, saved.zone)
            assertNull(viewModel.editor.value)
        }

    @Test
    fun `an invalid workout reports a message, keeps the sheet open and writes nothing`() =
        runTest {
            val repository = InMemoryFitnessRepository()
            val viewModel = viewModel(repository)
            viewModel.onAction(FitnessAction.OpenWorkout())

            viewModel.effects.test {
                viewModel.onAction(FitnessAction.Save(FitnessForm.Workout(cardioDraft(duration = ""), existing = null)))
                assertEquals(FitnessEffect.Message(R.string.fitness_error_invalid), awaitItem())
            }

            assertTrue(repository.current.workouts.isEmpty())
            assertNotNull(viewModel.editor.value)
        }

    @Test
    fun `a storage failure while saving shows the save error`() =
        runTest {
            val repository = InMemoryFitnessRepository().apply { failWrites = true }
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FitnessAction.Save(FitnessForm.Workout(cardioDraft(), existing = null)))
                assertEquals(FitnessEffect.Message(R.string.fitness_error_save), awaitItem())
            }
        }

    @Test
    fun `deleting offers undo and undo restores the workout`() =
        runTest {
            val workout = run("a")
            val repository = InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(workout)))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FitnessAction.Delete(FitnessItem.Workout(workout)))
                val deleted = awaitItem() as FitnessEffect.Deleted
                assertEquals(R.string.fitness_deleted_workout, deleted.messageRes)
                assertTrue(repository.current.workouts.isEmpty())

                viewModel.onAction(FitnessAction.Undo(deleted.item))
                advanceUntilIdle()
            }

            assertEquals(listOf(workout), repository.current.workouts)
        }

    @Test
    fun `saving as a template stores the workout and a template with its title`() =
        runTest {
            val repository = InMemoryFitnessRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(
                    FitnessAction.Save(FitnessForm.Workout(cardioDraft().copy(saveAsTemplate = true), existing = null)),
                )
                awaitItem()
            }

            assertEquals(1, repository.current.workouts.size)
            assertEquals(
                "Intervalos",
                repository.current.templates
                    .single()
                    .name,
            )
        }

    @Test
    fun `editing keeps the original zone and source`() =
        runTest {
            val original = run("a").copy(zone = java.time.ZoneId.of("Asia/Tokyo"))
            val repository = InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(original)))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                val draft = original.toDraft().copy(durationText = "55")
                viewModel.onAction(FitnessAction.Save(FitnessForm.Workout(draft, existing = original)))
                awaitItem()
            }

            val saved = repository.current.workouts.single()
            assertEquals("Asia/Tokyo", saved.zone.id)
            assertEquals(55 * 60L, saved.durationSec)
            assertEquals(original.source, saved.source)
        }

    @Test
    fun `weight and steps are saved one per day and a moved date drops the old one`() =
        runTest {
            val old = BodyWeight(today.minusDays(3), 81_000)
            val repository = InMemoryFitnessRepository(FitnessDataSet(weights = listOf(old)))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FitnessAction.Save(FitnessForm.Weight(WeightDraft(today, "80,5"), existing = old)))
                assertEquals(FitnessEffect.Saved, awaitItem())
                viewModel.onAction(FitnessAction.Save(FitnessForm.Steps(StepsDraft(today, "7000"), existing = null)))
                assertEquals(FitnessEffect.Saved, awaitItem())
                viewModel.onAction(FitnessAction.Save(FitnessForm.Steps(StepsDraft(today, "9000"), existing = null)))
                assertEquals(FitnessEffect.Saved, awaitItem())
            }

            assertEquals(listOf(BodyWeight(today, 80_500)), repository.current.weights)
            assertEquals(listOf(DailySteps(today, 9_000)), repository.current.steps)
        }

    @Test
    fun `opening and closing a sheet is reflected in the editor state`() =
        runTest {
            val viewModel = viewModel(InMemoryFitnessRepository())

            viewModel.onAction(FitnessAction.Open(FitnessEditor.EditWeight()))
            assertEquals(FitnessEditor.EditWeight(), viewModel.editor.value)

            viewModel.onAction(FitnessAction.Open(null))
            assertNull(viewModel.editor.value)
        }
}
