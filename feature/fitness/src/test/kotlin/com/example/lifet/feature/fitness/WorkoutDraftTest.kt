package com.example.lifet.feature.fitness

import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WorkoutDraftTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 3, 10)

    private fun cardio(
        duration: String = "30",
        distance: String = "",
        energy: String = "",
    ) = WorkoutDraft(
        id = "id",
        type = WorkoutType.RUN,
        date = today,
        startMinute = 18 * 60,
        durationText = duration,
        distanceText = distance,
        energyText = energy,
    )

    private fun strength(vararg exercises: ExerciseDraft) =
        WorkoutDraft(
            id = "id",
            type = WorkoutType.STRENGTH,
            date = today,
            startMinute = 19 * 60,
            durationText = "50",
            exercises = exercises.toList(),
        )

    private fun valid(draft: WorkoutDraft): Workout = (draft.validate(zone) as DraftResult.Valid).value

    private fun issues(draft: WorkoutDraft): Set<DraftIssue> = draft.validate(zone).issues()

    @Test
    fun `a cardio draft converts to SI units`() {
        val workout = valid(cardio(duration = "45", distance = "8,5", energy = "520").copy(title = "  Parque  "))

        assertEquals(45 * 60L, workout.durationSec)
        assertEquals(8_500L, workout.distanceM)
        assertEquals(520, workout.energyKcal)
        assertEquals("Parque", workout.title)
        assertEquals(today.atTime(18, 0).atZone(zone).toInstant(), workout.start)
        assertEquals(zone, workout.zone)
        assertTrue(workout.exercises.isEmpty())
    }

    @Test
    fun `distance and energy are optional but must be valid when typed`() {
        assertNull(valid(cardio()).distanceM)
        assertEquals(setOf(DraftIssue.DISTANCE_INVALID), issues(cardio(distance = "abc")))
        assertEquals(setOf(DraftIssue.DISTANCE_INVALID), issues(cardio(distance = "0")))
        assertEquals(setOf(DraftIssue.ENERGY_INVALID), issues(cardio(energy = "-5")))
    }

    @Test
    fun `strength ignores distance typed before switching type`() {
        val workout = valid(strength(ExerciseDraft("Press", listOf(SetDraft("5", "60")))).copy(distanceText = "9"))

        assertNull(workout.distanceM)
    }

    @Test
    fun `duration is required and bounded to one day`() {
        assertEquals(setOf(DraftIssue.DURATION_MISSING), issues(cardio(duration = "")))
        assertEquals(setOf(DraftIssue.DURATION_INVALID), issues(cardio(duration = "0")))
        assertEquals(setOf(DraftIssue.DURATION_INVALID), issues(cardio(duration = "1441")))
        assertEquals(24 * 3_600L, valid(cardio(duration = "1440")).durationSec)
    }

    @Test
    fun `sets convert reps and kilos, and an empty weight is bodyweight`() {
        val workout =
            valid(
                strength(
                    ExerciseDraft("Press banca", listOf(SetDraft("5", "62,5"), SetDraft("5", "62,5"))),
                    ExerciseDraft("Dominadas", listOf(SetDraft("8", ""))),
                ),
            )

        assertEquals(
            listOf(
                ExerciseLog("Press banca", listOf(StrengthSet(5, 62_500), StrengthSet(5, 62_500))),
                ExerciseLog("Dominadas", listOf(StrengthSet(8, 0))),
            ),
            workout.exercises,
        )
    }

    @Test
    fun `fully blank exercises and sets are ignored`() {
        val workout =
            valid(
                strength(
                    ExerciseDraft("Press", listOf(SetDraft("5", "60"), SetDraft())),
                    ExerciseDraft(),
                ),
            )

        assertEquals(1, workout.exercises.size)
        assertEquals(
            1,
            workout.exercises
                .single()
                .sets.size,
        )
    }

    @Test
    fun `a half filled exercise is an error instead of being dropped silently`() {
        assertEquals(
            setOf(DraftIssue.EXERCISE_NAME_MISSING),
            issues(strength(ExerciseDraft("", listOf(SetDraft("5", "60"))))),
        )
        assertEquals(
            setOf(DraftIssue.EXERCISE_WITHOUT_SETS),
            issues(strength(ExerciseDraft("Press", listOf(SetDraft())))),
        )
        assertEquals(
            setOf(DraftIssue.SET_INVALID),
            issues(strength(ExerciseDraft("Press", listOf(SetDraft("", "60"))))),
        )
        assertEquals(
            setOf(DraftIssue.SET_INVALID),
            issues(strength(ExerciseDraft("Press", listOf(SetDraft("0", "60"))))),
        )
        assertEquals(
            setOf(DraftIssue.SET_INVALID),
            issues(strength(ExerciseDraft("Press", listOf(SetDraft("5", "mucho"))))),
        )
    }

    @Test
    fun `a strength workout with no exercises is still valid`() {
        assertTrue(valid(strength()).exercises.isEmpty())
    }

    @Test
    fun `long texts are trimmed to the limits`() {
        val workout =
            valid(
                strength(ExerciseDraft("E".repeat(500), listOf(SetDraft("5", "60"))))
                    .copy(title = "T".repeat(500), note = "N".repeat(5_000)),
            )

        assertEquals(FitnessRules.TITLE_MAX_LENGTH, workout.title.length)
        assertEquals(FitnessRules.NOTE_MAX_LENGTH, workout.note.length)
        assertEquals(
            FitnessRules.EXERCISE_NAME_MAX_LENGTH,
            workout.exercises
                .single()
                .name.length,
        )
    }

    @Test
    fun `saving as a template needs a title`() {
        assertEquals(setOf(DraftIssue.TEMPLATE_NEEDS_TITLE), issues(cardio().copy(saveAsTemplate = true)))
        assertTrue(issues(cardio().copy(saveAsTemplate = true, title = "Rodaje")).isEmpty())
    }

    @Test
    fun `an invalid draft reports every problem at once`() {
        assertEquals(
            setOf(DraftIssue.DURATION_MISSING, DraftIssue.DISTANCE_INVALID, DraftIssue.ENERGY_INVALID),
            issues(cardio(duration = "", distance = "x", energy = "y")),
        )
    }

    @Test
    fun `start moves back with the duration while it still ends now`() {
        val draft = WorkoutDraft(id = "i", date = today, startMinute = 20 * 60, autoEndMinute = 20 * 60)

        assertEquals(19 * 60 + 15, draft.withDuration("45").startMinute)
        assertEquals(20 * 60, draft.withDuration("").startMinute)
    }

    @Test
    fun `a workout logged just after midnight does not go negative`() {
        val draft = WorkoutDraft(id = "i", date = today, startMinute = 10, autoEndMinute = 10)

        assertEquals(0, draft.withDuration("90").startMinute)
    }

    @Test
    fun `setting the start by hand stops the automatic adjustment`() {
        val draft =
            WorkoutDraft(id = "i", date = today, startMinute = 20 * 60, autoEndMinute = 20 * 60)
                .withStart(7 * 60)
                .withDuration("30")

        assertEquals(7 * 60, draft.startMinute)
        assertNull(draft.autoEndMinute)
    }

    @Test
    fun `adding a set copies the previous one and respects the limit`() {
        var draft = strength(ExerciseDraft("Press", listOf(SetDraft("5", "60"))))

        draft = draft.addSet(0)
        assertEquals(listOf(SetDraft("5", "60"), SetDraft("5", "60")), draft.exercises.single().sets)

        repeat(FitnessRules.MAX_SETS_PER_EXERCISE) { draft = draft.addSet(0) }
        assertEquals(
            FitnessRules.MAX_SETS_PER_EXERCISE,
            draft.exercises
                .single()
                .sets.size,
        )
    }

    @Test
    fun `exercises can be added up to the limit and removed`() {
        var draft = strength()
        repeat(FitnessRules.MAX_EXERCISES + 5) { draft = draft.addExercise() }

        assertEquals(FitnessRules.MAX_EXERCISES, draft.exercises.size)
        assertFalse(draft.canAddExercise)
        assertEquals(FitnessRules.MAX_EXERCISES - 1, draft.removeExercise(0).exercises.size)
    }

    @Test
    fun `a suggested exercise fills the first blank one before adding a new one`() {
        val draft = strength(ExerciseDraft("Press", listOf(SetDraft("5", "60"))), ExerciseDraft())

        val named = draft.addExerciseNamed("Remo")

        assertEquals(listOf("Press", "Remo"), named.exercises.map { it.name })
        assertEquals(3, named.addExerciseNamed("Curl").exercises.size)
    }

    @Test
    fun `switching to strength offers a first empty exercise`() {
        assertEquals(1, cardio().withType(WorkoutType.STRENGTH).exercises.size)
        assertTrue(strength().withType(WorkoutType.RUN).exercises.isEmpty())
    }

    @Test
    fun `editing a stored workout round trips through the draft`() {
        val original =
            Workout(
                id = "w",
                type = WorkoutType.STRENGTH,
                title = "Empuje",
                start = today.atTime(18, 30).atZone(zone).toInstant(),
                zone = zone,
                durationSec = 55 * 60L,
                energyKcal = 300,
                exercises =
                    listOf(
                        ExerciseLog("Press banca", listOf(StrengthSet(5, 82_500), StrengthSet(5, 82_500))),
                        ExerciseLog("Fondos", listOf(StrengthSet(10, 0))),
                    ),
                note = "bien",
            )

        assertEquals(original, valid(original.toDraft()))
    }

    @Test
    fun `a cardio workout with distance round trips through the draft`() {
        val original =
            Workout(
                id = "r",
                type = WorkoutType.RUN,
                title = "",
                start = today.atTime(7, 5).atZone(zone).toInstant(),
                zone = zone,
                durationSec = 38 * 60L,
                distanceM = 7_250,
            )

        assertEquals(original, valid(original.toDraft()))
    }

    @Test
    fun `repeating a workout moves it to today ending now with a new id`() {
        val original =
            Workout(
                id = "old",
                type = WorkoutType.RUN,
                title = "Parque",
                start = Instant.parse("2026-01-01T10:00:00Z"),
                zone = zone,
                durationSec = 40 * 60L,
                distanceM = 6_000,
            )

        val draft = original.toRepeatDraft("new", today, nowMinute = 12 * 60)

        assertEquals("new", draft.id)
        assertEquals(today, draft.date)
        assertEquals(11 * 60 + 20, draft.startMinute)
        assertEquals(12 * 60, draft.autoEndMinute)
        assertEquals("6", draft.distanceText)
        assertEquals("40", draft.durationText)
    }

    @Test
    fun `a template becomes a draft with its sets already filled`() {
        val template =
            WorkoutTemplate(
                id = "t",
                name = "Pierna",
                type = WorkoutType.STRENGTH,
                durationSec = 3_600,
                exercises = listOf(TemplateExercise("Sentadilla", 4, 5, 100_000), TemplateExercise("Plancha", 2, 1, 0)),
            )

        val draft = template.toDraft("new", today, nowMinute = 21 * 60)

        assertEquals("Pierna", draft.title)
        assertEquals(
            4,
            draft.exercises
                .first()
                .sets.size,
        )
        assertEquals(
            SetDraft("5", "100"),
            draft.exercises
                .first()
                .sets
                .first(),
        )
        assertEquals(
            SetDraft("1", ""),
            draft.exercises
                .last()
                .sets
                .first(),
        )
        assertEquals(20 * 60, draft.startMinute)
        assertEquals("60", draft.durationText)
    }

    @Test
    fun `a stored workout becomes a template with set counts and the heaviest load`() {
        val workout =
            Workout(
                id = "w",
                type = WorkoutType.STRENGTH,
                title = "Empuje",
                start = Instant.EPOCH,
                zone = zone,
                durationSec = 3_000,
                exercises =
                    listOf(
                        ExerciseLog("Press", listOf(StrengthSet(5, 60_000), StrengthSet(5, 65_000))),
                        ExerciseLog("Vacío", emptyList()),
                    ),
            )

        val template = workout.toTemplate("t")

        assertEquals(listOf(TemplateExercise("Press", 2, 5, 65_000)), template.exercises)
        assertEquals("Empuje", template.name)
    }
}
