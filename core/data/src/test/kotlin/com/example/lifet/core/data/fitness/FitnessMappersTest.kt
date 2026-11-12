package com.example.lifet.core.data.fitness

import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FitnessMappersTest {
    private val zone = ZoneId.of("Europe/Madrid")

    private fun workout(vararg exercises: ExerciseLog) =
        Workout(
            id = "w",
            type = WorkoutType.STRENGTH,
            title = "Empuje 💪",
            start = Instant.parse("2026-03-09T18:00:00Z"),
            zone = zone,
            durationSec = 3_600,
            exercises = exercises.toList(),
        )

    @Test
    fun `sets are flattened with exercise and set order and rebuilt identically`() {
        val original =
            workout(
                ExerciseLog(
                    "Press banca",
                    listOf(StrengthSet(5, 80_000), StrengthSet(5, 82_500), StrengthSet(3, 85_000)),
                ),
                ExerciseLog("Dominadas", listOf(StrengthSet(8, 0))),
            )

        val rows = original.toSetEntities()
        val rebuilt = original.toEntity().toDomain(rows.toExerciseLogs())

        assertEquals(listOf(0, 0, 0, 1), rows.map { it.exerciseIndex })
        assertEquals(listOf(0, 1, 2, 0), rows.map { it.setIndex })
        assertEquals(original, rebuilt)
    }

    @Test
    fun `a workout without exercises produces no rows`() {
        assertTrue(workout().toSetEntities().isEmpty())
        assertTrue(emptyList<com.example.lifet.core.database.fitness.FitnessSetEntity>().toExerciseLogs().isEmpty())
    }

    @Test
    fun `the same exercise twice in a workout stays as two separate blocks`() {
        val original =
            workout(
                ExerciseLog("Press", listOf(StrengthSet(5, 60_000))),
                ExerciseLog("Remo", listOf(StrengthSet(10, 40_000))),
                ExerciseLog("Press", listOf(StrengthSet(8, 50_000))),
            )

        val logs = original.toSetEntities().toExerciseLogs()

        assertEquals(listOf("Press", "Remo", "Press"), logs.map { it.name })
    }

    @Test
    fun `template exercises keep their position`() {
        val template =
            WorkoutTemplate(
                id = "t",
                name = "Pierna",
                type = WorkoutType.STRENGTH,
                durationSec = 3_600,
                exercises =
                    listOf(
                        TemplateExercise("Sentadilla", 4, 6, 100_000),
                        TemplateExercise("Prensa", 3, 12, 160_000),
                    ),
            )

        val rows = template.toExerciseEntities()
        val rebuilt = template.toEntity().toDomain(rows.reversed())

        assertEquals(listOf(0, 1), rows.map { it.position })
        assertEquals(template, rebuilt)
    }
}
