package com.example.lifet.core.database.fitness

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.model.fitness.RecordSource
import com.example.lifet.core.model.fitness.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class FitnessDaoTest {
    private lateinit var database: LifeTrackerDatabase
    private lateinit var dao: FitnessDao
    private val day = LocalDate.of(2026, 3, 10)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
        dao = database.fitnessDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun workout(
        id: String,
        start: String,
        title: String = "",
    ) = FitnessWorkoutEntity(
        id = id,
        type = WorkoutType.STRENGTH,
        title = title,
        start = Instant.parse(start),
        zoneId = ZoneId.of("Europe/Madrid"),
        durationSec = 3_600,
        distanceM = null,
        energyKcal = null,
        note = "",
        source = RecordSource.MANUAL,
        externalId = null,
    )

    private fun set(
        workoutId: String,
        exercise: Int,
        index: Int,
        reps: Int = 5,
        grams: Long = 60_000,
        name: String = "Press banca",
    ) = FitnessSetEntity(
        workoutId = workoutId,
        exerciseIndex = exercise,
        exerciseName = name,
        setIndex = index,
        reps = reps,
        weightGrams = grams,
    )

    @Test
    fun `workouts come newest first and keep zone, long text and emojis`() =
        runTest {
            dao.saveWorkout(workout("old", "2026-03-01T18:00:00Z"), emptyList())
            dao.saveWorkout(workout("new", "2026-03-09T18:00:00Z", title = "Pierna 🦵 ".repeat(50)), emptyList())

            val items = dao.observeWorkouts().first()

            assertEquals(listOf("new", "old"), items.map { it.id })
            assertEquals(ZoneId.of("Europe/Madrid"), items.first().zoneId)
            assertTrue(items.first().title.startsWith("Pierna 🦵"))
        }

    @Test
    fun `sets come back in exercise and set order`() =
        runTest {
            dao.saveWorkout(
                workout("w", "2026-03-09T18:00:00Z"),
                listOf(
                    set("w", exercise = 1, index = 0, name = "Remo"),
                    set("w", exercise = 0, index = 1),
                    set("w", exercise = 0, index = 0),
                ),
            )

            val sets = dao.observeSets().first()

            assertEquals(listOf("Press banca", "Press banca", "Remo"), sets.map { it.exerciseName })
            assertEquals(listOf(0, 1, 0), sets.map { it.setIndex })
        }

    @Test
    fun `saving an edited workout replaces its sets instead of piling them up`() =
        runTest {
            dao.saveWorkout(workout("w", "2026-03-09T18:00:00Z"), listOf(set("w", 0, 0), set("w", 0, 1)))

            dao.saveWorkout(workout("w", "2026-03-09T18:00:00Z", title = "Editado"), listOf(set("w", 0, 0, reps = 8)))

            assertEquals(1, dao.observeWorkouts().first().size)
            val sets = dao.observeSets().first()
            assertEquals(listOf(8), sets.map { it.reps })
        }

    @Test
    fun `deleting a workout cascades to its sets only`() =
        runTest {
            dao.saveWorkout(workout("a", "2026-03-09T18:00:00Z"), listOf(set("a", 0, 0)))
            dao.saveWorkout(workout("b", "2026-03-08T18:00:00Z"), listOf(set("b", 0, 0)))

            dao.deleteWorkout("a")

            assertEquals(listOf("b"), dao.observeWorkouts().first().map { it.id })
            assertEquals(listOf("b"), dao.observeSets().first().map { it.workoutId })
        }

    @Test
    fun `a second weight on the same day replaces the first`() =
        runTest {
            dao.observeWeights().test {
                assertTrue(awaitItem().isEmpty())
                dao.upsertWeight(FitnessWeightEntity(day, 80_000, ""))
                awaitItem()
                dao.upsertWeight(FitnessWeightEntity(day, 79_500, "tras correr"))
                val weights = awaitItem()
                assertEquals(1, weights.size)
                assertEquals(79_500L, weights.single().weightGrams)
            }
        }

    @Test
    fun `steps are one per day and can be deleted`() =
        runTest {
            dao.upsertSteps(FitnessStepsEntity(day, 5_000, RecordSource.MANUAL))
            dao.upsertSteps(FitnessStepsEntity(day, 7_200, RecordSource.MANUAL))
            assertEquals(7_200, dao.observeSteps().first().single().steps)

            dao.deleteSteps(day)

            assertTrue(dao.observeSteps().first().isEmpty())
        }

    @Test
    fun `template exercises keep their position and die with the template`() =
        runTest {
            val template = FitnessTemplateEntity("t", "Push", WorkoutType.STRENGTH, 3_600)
            dao.saveTemplate(
                template,
                listOf(
                    FitnessTemplateExerciseEntity(templateId = "t", position = 1, name = "Fondos", sets = 3, reps = 10, weightGrams = 0),
                    FitnessTemplateExerciseEntity(templateId = "t", position = 0, name = "Press", sets = 4, reps = 6, weightGrams = 70_000),
                ),
            )

            assertEquals(listOf("Press", "Fondos"), dao.observeTemplateExercises().first().map { it.name })

            dao.deleteTemplate("t")

            assertTrue(dao.observeTemplates().first().isEmpty())
            assertTrue(dao.observeTemplateExercises().first().isEmpty())
        }

    @Test
    fun `replaceAll wipes previous data in every table`() =
        runTest {
            dao.saveWorkout(workout("old", "2026-03-01T18:00:00Z"), listOf(set("old", 0, 0)))
            dao.upsertWeight(FitnessWeightEntity(day, 80_000, ""))
            dao.upsertSteps(FitnessStepsEntity(day, 5_000, RecordSource.MANUAL))
            dao.saveTemplate(FitnessTemplateEntity("t", "Push", WorkoutType.STRENGTH, 3_600), emptyList())

            dao.replaceAll(
                workouts = listOf(workout("new", "2026-03-09T18:00:00Z")),
                sets = listOf(set("new", 0, 0)),
                weights = emptyList(),
                steps = emptyList(),
                templates = emptyList(),
                templateExercises = emptyList(),
            )

            assertEquals(listOf("new"), dao.observeWorkouts().first().map { it.id })
            assertEquals(listOf("new"), dao.observeSets().first().map { it.workoutId })
            assertTrue(dao.observeWeights().first().isEmpty())
            assertTrue(dao.observeSteps().first().isEmpty())
            assertTrue(dao.observeTemplates().first().isEmpty())
        }
}
