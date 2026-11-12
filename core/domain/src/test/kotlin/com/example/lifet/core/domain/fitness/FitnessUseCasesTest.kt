package com.example.lifet.core.domain.fitness

import app.cash.turbine.test
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutType
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.fitness.InMemoryFitnessRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FitnessUseCasesTest {
    private val clock = TestClock.fixed("2026-03-11T09:00:00Z")
    private val zone = clock.zone
    private val today = LocalDate.of(2026, 3, 11)

    private fun run(
        id: String,
        date: LocalDate,
        minutes: Long,
    ) = Workout(
        id = id,
        type = WorkoutType.RUN,
        title = "",
        start = date.atTime(18, 0).atZone(zone).toInstant(),
        zone = zone,
        durationSec = minutes * 60,
    )

    @Test
    fun `today summary of an unused area has no data`() =
        runTest {
            ObserveFitnessTodaySummary(InMemoryFitnessRepository(), clock)().test {
                val summary = awaitItem()
                assertFalse(summary.hasData)
                assertEquals(0, summary.weekActiveMinutes)
                assertEquals(150, summary.weeklyGoalMinutes)
                assertFalse(summary.workedOutToday)
                assertNull(summary.latestWeightGrams)
            }
        }

    @Test
    fun `today summary reacts to a new workout and weight`() =
        runTest {
            val repository = InMemoryFitnessRepository()

            ObserveFitnessTodaySummary(repository, clock)().test {
                awaitItem()
                repository.saveWorkout(run("a", today, 40))
                val afterWorkout = awaitItem()
                assertTrue(afterWorkout.hasData)
                assertTrue(afterWorkout.workedOutToday)
                assertEquals(40, afterWorkout.weekActiveMinutes)

                repository.saveWeight(BodyWeight(today, 80_500))
                assertEquals(80_500L, awaitItem().latestWeightGrams)
            }
        }

    @Test
    fun `today summary does not emit again when nothing it shows changed`() =
        runTest {
            val repository = InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(run("a", today, 30))))

            ObserveFitnessTodaySummary(repository, clock)().test {
                awaitItem()
                repository.saveSteps(DailySteps(today, 4_000))
                assertEquals(4_000, awaitItem().stepsToday)
                // Pasos de otro día: no cambian nada de lo que muestra Hoy.
                repository.saveSteps(DailySteps(today.minusDays(6), 4_000))
                expectNoEvents()
            }
        }

    @Test
    fun `series covers the range up to today with zeros on rest days`() =
        runTest {
            val repository =
                InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(run("a", today.minusDays(1), 45))))
            val range = DateRange.containing(DateRangeUnit.WEEK, today)

            ObserveFitnessSeries(repository, clock)(range).test {
                val series = awaitItem()
                assertEquals(3, series.days.size)
                assertEquals(listOf(0, 45, 0), series.days.map { it.activeMinutes })
                assertEquals(45, series.totalMinutes)
            }
        }

    @Test
    fun `overview exposes twelve weeks and the history`() =
        runTest {
            val repository = InMemoryFitnessRepository(FitnessDataSet(workouts = listOf(run("a", today, 30))))

            ObserveFitnessOverview(repository, clock)().test {
                val overview = awaitItem()
                assertEquals(12, overview.weeks.size)
                assertEquals(30, overview.weeks.last().minutes)
                assertTrue(overview.hasAnyRecord)
            }
        }

    @Test
    fun `a failing source surfaces as an exception for the view model to catch`() =
        runTest {
            val repository = InMemoryFitnessRepository().apply { failObserve = true }

            ObserveFitnessOverview(repository, clock)().test {
                assertTrue(awaitError() is IllegalStateException)
            }
        }
}
