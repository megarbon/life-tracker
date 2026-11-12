@file:Suppress("MagicNumber") // Duraciones y horas literales: son datos de ejemplo, no reglas.

package com.example.lifet.core.demo.fitness

import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Entreno que empieza a la hora local [time] de [date] en la zona [zone]. */
internal fun demoWorkout(
    id: String,
    type: WorkoutType,
    title: String,
    date: LocalDate,
    time: LocalTime,
    zone: ZoneId,
    durationSec: Long,
    distanceM: Long? = null,
    energyKcal: Int? = null,
    exercises: List<ExerciseLog> = emptyList(),
    note: String = "",
): Workout =
    Workout(
        id = id,
        type = type,
        title = title,
        start = date.atTime(time).atZone(zone).toInstant(),
        zone = zone,
        durationSec = durationSec,
        distanceM = distanceM,
        energyKcal = energyKcal,
        exercises = exercises,
        note = note,
    )

/** [count] series iguales. */
internal fun repeatedSets(
    count: Int,
    reps: Int,
    weightGrams: Long,
): List<StrengthSet> = List(count) { StrengthSet(reps, weightGrams) }

/** Repite [seed] hasta acercarse a [max] caracteres sin pasarse, y sin partir nunca un emoji por la mitad. */
internal fun fitText(
    seed: String,
    max: Int,
): String =
    buildString {
        while (length + seed.length <= max) append(seed)
    }

/**
 * Quita lo que aún no ha ocurrido respecto al reloj: un entreno cuyo final queda en el futuro, o un peso o unos
 * pasos de mañana. Los datos de demo nunca deben adelantarse a "ahora".
 */
internal fun FitnessDataSet.withoutFuture(clock: Clock): FitnessDataSet {
    val now = clock.instant()
    val today = LocalDate.now(clock)
    return copy(
        workouts = workouts.filter { !it.end.isAfter(now) },
        weights = weights.filter { it.date <= today },
        steps = steps.filter { it.date <= today },
    )
}
