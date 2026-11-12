package com.example.lifet.core.demo.fitness

import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.fitness.ExerciseAnalytics
import com.example.lifet.core.model.fitness.FitnessCalculator
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.WorkoutType
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.fitness.InMemoryFitnessRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FitnessDemoDataGeneratorTest {
    private val clock = TestClock.fixed("2026-03-10T15:00:00Z")
    private val today = LocalDate.parse("2026-03-10")

    private suspend fun load(scenario: DemoScenario): FitnessDataSet {
        val repository = InMemoryFitnessRepository()
        FitnessDemoDataGenerator(repository).seed(scenario, DemoContext(clock))
        return repository.current
    }

    /** Días en los que hay algún dato de cualquier tipo, contando cada día que toca un entreno. */
    private fun activeDays(data: FitnessDataSet): Set<LocalDate> =
        data.workouts.flatMap { it.secondsByDay().keys }.toSet() + data.weights.map { it.date } +
            data.steps.map { it.date }

    @Test
    fun `area is fitness`() {
        assertEquals(Area.FITNESS, FitnessDemoDataGenerator(InMemoryFitnessRepository()).area)
    }

    @Test
    fun `empty scenario leaves nothing even after a regular load`() =
        runTest {
            val repository = InMemoryFitnessRepository()
            val generator = FitnessDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.seed(DemoScenario.EMPTY, DemoContext(clock))

            assertTrue(repository.current.workouts.isEmpty() && repository.current.weights.isEmpty())
            assertTrue(repository.current.steps.isEmpty() && repository.current.templates.isEmpty())
        }

    @Test
    fun `clear removes everything`() =
        runTest {
            val repository = InMemoryFitnessRepository()
            val generator = FitnessDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.clear()

            assertTrue(repository.current.hasNoRecords)
        }

    @Test
    fun `new user has about three days of data`() =
        runTest {
            val data = load(DemoScenario.NEW_USER)

            assertEquals(setOf(today.minusDays(2), today.minusDays(1), today), activeDays(data))
            assertTrue(data.workouts.any { it.type == WorkoutType.STRENGTH && it.exercises.isNotEmpty() })
        }

    @Test
    fun `seeding twice gives the same data`() =
        runTest {
            assertEquals(load(DemoScenario.REGULAR), load(DemoScenario.REGULAR))
            assertEquals(load(DemoScenario.EDGE_CASES), load(DemoScenario.EDGE_CASES))
        }

    @Test
    fun `no scenario puts anything in the future`() =
        runTest {
            DemoScenario.entries.forEach { scenario ->
                val data = load(scenario)
                assertTrue("$scenario", data.workouts.none { it.end.isAfter(clock.instant()) })
                assertTrue("$scenario", data.weights.none { it.date > today })
                assertTrue("$scenario", data.steps.none { it.date > today })
            }
        }

    @Test
    fun `regular covers about six months with three or four workouts a week`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val weeks = FitnessCalculator(data, today).overview().weeks
            val first = data.workouts.minOf { it.startDate }

            assertTrue("empieza hace al menos 150 días", first <= today.minusDays(150))
            assertTrue("hay datos de peso casi a diario", data.weights.size >= 120)
            val full = weeks.dropLast(1).filter { it.workouts > 0 }
            assertTrue(full.all { it.workouts in 3..4 })
            // Unas semanas cumplen los 150 min y otras no: la gráfica y el héroe deben mostrar ambos casos.
            assertTrue("hay semanas que cumplen", full.count { it.goalReached } >= full.size / 3)
            assertTrue("hay semanas que no cumplen", full.count { !it.goalReached } >= full.size / 3)
        }

    @Test
    fun `regular has exactly one vacation week without any activity`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val calculator = FitnessCalculator(data, today)
            val empty =
                calculator
                    .overview()
                    .weeks
                    .dropLast(1)
                    .filter { it.minutes == 0 }

            assertEquals(1, empty.size)
            val vacation = (0L until 7L).map { empty.single().weekStart.plusDays(it) }
            assertTrue(data.steps.none { it.date in vacation })
            assertTrue(data.workouts.none { it.startDate in vacation })
        }

    @Test
    fun `regular loads progress believably and break personal records`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val history = ExerciseAnalytics.history(data.workouts)

            val bench = history.exercises.first { it.key == "press banca" }
            val first = bench.sessions.first().bestOneRepMaxGrams ?: 0L
            val last = bench.sessions.last().bestOneRepMaxGrams ?: 0L
            assertTrue("press banca sube de $first a $last", last > first + 8_000L)
            assertTrue("no es una subida irreal", last < first * 2)
            assertTrue(history.records.size >= 20)

            val pullUps = history.exercises.first { it.key == "dominadas" }
            assertTrue(pullUps.isBodyweightOnly)
            assertTrue((pullUps.bestReps ?: 0) > (pullUps.sessions.first().bestBodyweightReps ?: 0))
        }

    @Test
    fun `regular weight drifts down smoothly and stays in a believable range`() =
        runTest {
            val weights = load(DemoScenario.REGULAR).weights.sortedBy { it.date }
            val firstMonth = weights.take(30).map { it.weightGrams }.average()
            val lastMonth = weights.takeLast(30).map { it.weightGrams }.average()

            assertTrue("baja de ${firstMonth.toLong()} a ${lastMonth.toLong()}", lastMonth < firstMonth - 1_500)
            assertTrue(weights.all { it.weightGrams in 70_000..90_000 })
            assertTrue(weights.zipWithNext().all { (a, b) -> kotlin.math.abs(a.weightGrams - b.weightGrams) < 2_500 })
        }

    @Test
    fun `regular offers templates and every set is valid`() =
        runTest {
            val data = load(DemoScenario.REGULAR)

            assertTrue(data.templates.size >= 3)
            assertTrue(data.workouts.flatMap { it.exercises }.all { exercise -> exercise.sets.all { it.reps >= 1 } })
            assertTrue(data.workouts.all { it.durationSec > 0 })
        }

    @Test
    fun `edge cases cross midnight and keep the extremes`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)

            assertTrue(data.workouts.any { it.secondsByDay().size > 1 })
            assertTrue(data.workouts.any { it.title.length > FitnessRules.TITLE_MAX_LENGTH - 60 && "🦵" in it.title })
            assertTrue(data.workouts.any { it.note.length > FitnessRules.NOTE_MAX_LENGTH - 80 })
            assertTrue(data.workouts.any { it.durationSec == FitnessRules.MAX_DURATION_SECONDS })
            assertTrue(
                data.workouts.flatMap { it.exercises }.flatMap { it.sets }.any {
                    it.weightGrams == FitnessRules.MAX_SET_WEIGHT_GRAMS
                },
            )
            assertTrue(data.workouts.groupBy { it.startDate }.any { it.value.size >= 8 })
            assertTrue(data.workouts.any { it.zone.id == "Asia/Tokyo" })
        }

    @Test
    fun `edge cases respect the limits the form accepts`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)
            val exercises = data.workouts.flatMap { it.exercises }

            assertTrue(data.workouts.all { it.title.length <= FitnessRules.TITLE_MAX_LENGTH })
            assertTrue(data.workouts.all { it.note.length <= FitnessRules.NOTE_MAX_LENGTH })
            assertTrue(data.workouts.all { it.durationSec in 1..FitnessRules.MAX_DURATION_SECONDS })
            assertTrue(data.workouts.all { (it.distanceM ?: 0L) <= FitnessRules.MAX_DISTANCE_METERS })
            assertTrue(data.workouts.all { (it.energyKcal ?: 0) <= FitnessRules.MAX_ENERGY_KCAL })
            assertTrue(data.workouts.all { it.exercises.size <= FitnessRules.MAX_EXERCISES })
            assertTrue(exercises.all { it.name.length <= FitnessRules.EXERCISE_NAME_MAX_LENGTH })
            assertTrue(exercises.all { it.sets.size <= FitnessRules.MAX_SETS_PER_EXERCISE })
            assertTrue(exercises.flatMap { it.sets }.all { it.reps in 1..FitnessRules.MAX_REPS })
            assertTrue(
                data.weights.all {
                    it.weightGrams in
                        FitnessRules.MIN_BODY_WEIGHT_GRAMS..FitnessRules.MAX_BODY_WEIGHT_GRAMS
                },
            )
            assertTrue(data.steps.all { it.steps in 0..FitnessRules.MAX_STEPS })
        }

    @Test
    fun `edge cases have a gap without data and two spellings of one exercise merged`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)
            val days = activeDays(data)

            assertTrue((14L..25L).none { today.minusDays(it) in days })
            val bench = ExerciseAnalytics.history(data.workouts).exercises.single { it.key == "press banca" }
            assertEquals(2, bench.sessions.size)
            assertEquals("Press  Banca".trim().replace("  ", " ").lowercase(), bench.key)
        }
}
