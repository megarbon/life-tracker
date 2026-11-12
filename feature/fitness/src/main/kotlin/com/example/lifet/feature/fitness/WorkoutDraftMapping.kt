package com.example.lifet.feature.fitness

import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.Workout
import java.time.ZoneId

private const val KG_SCALE = 3
private const val KM_SCALE = 3

/**
 * Valida el borrador y lo convierte en [Workout]. [zone] es la del entreno que se edita o la del dispositivo si es
 * nuevo. Las series y ejercicios totalmente vacíos se ignoran; los a medias son un error, nunca se descartan en
 * silencio.
 */
internal fun WorkoutDraft.validate(zone: ZoneId): DraftResult<Workout> {
    val collector = IssueCollector()
    val minutes =
        collector.whole(
            durationText,
            1..(FitnessRules.MAX_DURATION_SECONDS / FitnessRules.SECONDS_PER_MINUTE).toInt(),
            DraftIssue.DURATION_MISSING,
            DraftIssue.DURATION_INVALID,
        )
    val distance = if (type.isCardio) collector.distance(distanceText) else null
    val kcal = collector.energy(energyText)
    val exercises = if (type.isStrength) collector.exercises(exercises) else emptyList()
    collector.require(!saveAsTemplate || title.isNotBlank(), DraftIssue.TEMPLATE_NEEDS_TITLE)
    return collector.result {
        minutes?.let {
            Workout(
                id = id,
                type = type,
                title = title.trim().take(FitnessRules.TITLE_MAX_LENGTH),
                start = date.atTime(startTime).atZone(zone).toInstant(),
                zone = zone,
                durationSec = it * FitnessRules.SECONDS_PER_MINUTE,
                distanceM = distance,
                energyKcal = kcal,
                exercises = exercises,
                note = note.trim().take(FitnessRules.NOTE_MAX_LENGTH),
            )
        }
    }
}

/** Kilómetros tecleados a metros; vacío es "sin distancia". */
private fun IssueCollector.distance(text: String): Long? {
    if (text.isBlank()) return null
    val meters = parseScaled(text, KM_SCALE)?.takeIf { it in 1..FitnessRules.MAX_DISTANCE_METERS }
    if (meters == null) add(DraftIssue.DISTANCE_INVALID)
    return meters
}

private fun IssueCollector.energy(text: String): Int? {
    if (text.isBlank()) return null
    val kcal = parseWholeNumber(text)?.takeIf { it in 1..FitnessRules.MAX_ENERGY_KCAL }
    if (kcal == null) add(DraftIssue.ENERGY_INVALID)
    return kcal
}

private fun IssueCollector.exercises(drafts: List<ExerciseDraft>): List<ExerciseLog> =
    drafts.filterNot { it.isBlank }.mapNotNull { exercise ->
        val name = exercise.name.trim().take(FitnessRules.EXERCISE_NAME_MAX_LENGTH)
        val filled = exercise.filledSets
        require(name.isNotEmpty(), DraftIssue.EXERCISE_NAME_MISSING)
        require(filled.isNotEmpty(), DraftIssue.EXERCISE_WITHOUT_SETS)
        val sets = filled.mapNotNull { set(it) }
        if (name.isNotEmpty() && sets.isNotEmpty() && sets.size == filled.size) ExerciseLog(name, sets) else null
    }

private fun IssueCollector.set(draft: SetDraft): StrengthSet? {
    val reps = parseWholeNumber(draft.repsText)?.takeIf { it in 1..FitnessRules.MAX_REPS }
    val grams = if (draft.weightText.isBlank()) 0L else parseScaled(draft.weightText, KG_SCALE)
    val validWeight = grams != null && grams <= FitnessRules.MAX_SET_WEIGHT_GRAMS
    require(reps != null && validWeight, DraftIssue.SET_INVALID)
    return if (reps != null && grams != null && validWeight) StrengthSet(reps, grams) else null
}
