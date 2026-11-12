package com.example.lifet.core.model.fitness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseAnalyticsTest {
    private val day1 = TODAY.minusDays(14)
    private val day2 = TODAY.minusDays(7)
    private val day3 = TODAY

    @Test
    fun `key ignores case, surrounding spaces and repeated spaces`() {
        assertEquals(ExerciseAnalytics.key("Press  Banca "), ExerciseAnalytics.key("press banca"))
    }

    @Test
    fun `the first time an exercise is done is not a record`() {
        val history = ExerciseAnalytics.history(listOf(strength("a", day1, lift("Sentadilla", 5 to 100_000L))))

        assertTrue(history.records.isEmpty())
        assertEquals(1, history.exercises.single().sessions.size)
    }

    @Test
    fun `beating the previous best estimated max is a record, matching it is not`() {
        val history =
            ExerciseAnalytics.history(
                listOf(
                    strength("a", day1, lift("Sentadilla", 5 to 100_000L)),
                    strength("b", day2, lift("Sentadilla", 5 to 105_000L)),
                    strength("c", day3, lift("Sentadilla", 5 to 105_000L)),
                ),
            )

        val record = history.records.single()
        assertEquals("b", record.workoutId)
        assertEquals(RecordKind.ONE_REP_MAX, record.kind)
        assertEquals(105_000L * 35 / 30, record.value)
        assertEquals(listOf(record), history.recordsByWorkout["b"])
    }

    @Test
    fun `more reps at the same load also beats the estimated max`() {
        val history =
            ExerciseAnalytics.history(
                listOf(
                    strength("a", day1, lift("Remo", 8 to 50_000L)),
                    strength("b", day2, lift("Remo", 10 to 50_000L)),
                ),
            )

        assertEquals("b", history.records.single().workoutId)
    }

    @Test
    fun `a lighter heavier mix keeps only the best set of the session`() {
        val progress =
            ExerciseAnalytics
                .history(listOf(strength("a", day1, lift("Press", 10 to 40_000L, 3 to 70_000L))))
                .exercises
                .single()

        assertEquals(70_000L * 33 / 30, progress.bestOneRepMaxGrams)
        assertEquals(70_000L, progress.sessions.single().topWeightGrams)
    }

    @Test
    fun `bodyweight exercises are tracked by reps`() {
        val history =
            ExerciseAnalytics.history(
                listOf(
                    strength("a", day1, lift("Dominadas", 6 to 0L)),
                    strength("b", day2, lift("Dominadas", 8 to 0L)),
                ),
            )

        val progress = history.exercises.single()
        assertTrue(progress.isBodyweightOnly)
        assertNull(progress.bestOneRepMaxGrams)
        assertEquals(8, progress.bestReps)
        assertEquals(RecordKind.BODYWEIGHT_REPS, history.records.single().kind)
        assertEquals(8L, history.records.single().value)
    }

    @Test
    fun `same exercise written two ways is merged and shows the latest spelling`() {
        val history =
            ExerciseAnalytics.history(
                listOf(
                    strength("a", day1, lift("press banca", 5 to 60_000L)),
                    strength("b", day2, lift("Press  Banca", 5 to 62_500L)),
                ),
            )

        val progress = history.exercises.single()
        assertEquals("Press  Banca".trim(), progress.name)
        assertEquals(2, progress.sessions.size)
    }

    @Test
    fun `sessions come in chronological order whatever the input order`() {
        val history =
            ExerciseAnalytics.history(
                listOf(
                    strength("late", day3, lift("Peso muerto", 5 to 120_000L)),
                    strength("early", day1, lift("Peso muerto", 5 to 100_000L)),
                ),
            )

        assertEquals(listOf("early", "late"), history.exercises.single().sessions.map { it.workoutId })
        assertEquals("late", history.records.single().workoutId)
    }

    @Test
    fun `cardio workouts and empty exercises are ignored`() {
        val history =
            ExerciseAnalytics.history(
                listOf(cardio("run", day1, 30), strength("blank", day2, lift("  ", 5 to 10_000L))),
            )

        assertTrue(history.exercises.isEmpty())
    }

    @Test
    fun `sets with zero reps are discarded`() {
        val history = ExerciseAnalytics.history(listOf(strength("a", day1, lift("Press", 0 to 60_000L))))

        assertTrue(history.exercises.isEmpty())
    }
}
