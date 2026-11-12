package com.example.lifet.feature.fitness

import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * Puntos de partida de un borrador: editar un entreno guardado, repetir uno anterior, aplicar una plantilla o
 * empezar en blanco. Son el camino inverso a la validación (de dominio a texto tecleable) y son puros.
 */

private const val KG_SCALE = 3
private const val KM_SCALE = 3

/** Minuto del día de este instante, en la zona del propio entreno. */
private fun Instant.minuteOfDayIn(zone: ZoneId): Int =
    atZone(zone).toLocalTime().let { it.hour * WorkoutDraft.MINUTES_IN_HOUR + it.minute }

/** Segundos a minutos enteros, redondeando al más cercano y nunca por debajo de uno. */
private fun Long.toMinutes(): Long =
    ((this + FitnessRules.SECONDS_PER_MINUTE / 2) / FitnessRules.SECONDS_PER_MINUTE).coerceAtLeast(1L)

/** Texto de una carga en kg; una carga de cero (peso corporal) se deja vacía, que es como se teclea. */
private fun Long.toLoadText(): String = if (this == 0L) "" else toEditableText(KG_SCALE)

private fun StrengthSet.toDraft(): SetDraft = SetDraft(reps.toString(), weightGrams.toLoadText())

/** Borrador de un entreno que se edita: conserva fecha, hora y zona del original. */
internal fun Workout.toDraft(): WorkoutDraft =
    prefill(this, id = id, date = startDate, startMinute = start.minuteOfDayIn(zone), autoEndMinute = null)

/** Borrador para repetir un entreno anterior hoy, acabando "ahora". */
internal fun Workout.toRepeatDraft(
    newId: String,
    today: LocalDate,
    nowMinute: Int,
): WorkoutDraft = prefill(this, newId, today, nowMinute - durationSec.toMinutes().toInt(), nowMinute)

private fun prefill(
    workout: Workout,
    id: String,
    date: LocalDate,
    startMinute: Int,
    autoEndMinute: Int?,
): WorkoutDraft =
    WorkoutDraft(
        id = id,
        type = workout.type,
        title = workout.title,
        date = date,
        startMinute = startMinute.coerceIn(0, WorkoutDraft.MINUTES_IN_DAY - 1),
        autoEndMinute = autoEndMinute,
        durationText = workout.durationSec.toMinutes().toString(),
        distanceText = workout.distanceM?.toEditableText(KM_SCALE).orEmpty(),
        energyText = workout.energyKcal?.toString().orEmpty(),
        exercises = workout.exercises.map { ExerciseDraft(it.name, it.sets.map(StrengthSet::toDraft)) },
        note = workout.note,
    )

/** Borrador a partir de una plantilla: sus series ya con las repeticiones y la carga habituales. */
internal fun WorkoutTemplate.toDraft(
    newId: String,
    today: LocalDate,
    nowMinute: Int,
): WorkoutDraft {
    val minutes = durationSec.toMinutes().toInt()
    return WorkoutDraft(
        id = newId,
        type = type,
        title = name,
        date = today,
        startMinute = (nowMinute - minutes).coerceIn(0, WorkoutDraft.MINUTES_IN_DAY - 1),
        autoEndMinute = nowMinute,
        durationText = minutes.toString(),
        exercises = exercises.map { ExerciseDraft(it.name, List(it.sets) { _ -> it.toSetDraft() }) },
    )
}

private fun TemplateExercise.toSetDraft(): SetDraft = SetDraft(reps.toString(), weightGrams.toLoadText())

/** Borrador vacío para un entreno nuevo que acaba "ahora". */
internal fun blankWorkoutDraft(
    id: String,
    type: WorkoutType,
    today: LocalDate,
    nowMinute: Int,
): WorkoutDraft =
    WorkoutDraft(
        id = id,
        type = type,
        date = today,
        startMinute = nowMinute.coerceIn(0, WorkoutDraft.MINUTES_IN_DAY - 1),
        autoEndMinute = nowMinute,
        exercises = if (type.isStrength) listOf(ExerciseDraft()) else emptyList(),
    )

/**
 * Plantilla a partir de un entreno guardado: cada ejercicio con su número de series, las repeticiones de la primera
 * y la carga más alta, que es la de referencia al repetir.
 */
internal fun Workout.toTemplate(templateId: String): WorkoutTemplate =
    WorkoutTemplate(
        id = templateId,
        name = title,
        type = type,
        durationSec = durationSec,
        exercises =
            exercises.mapNotNull { exercise ->
                exercise.sets.firstOrNull()?.let { first ->
                    TemplateExercise(
                        name = exercise.name,
                        sets = exercise.sets.size,
                        reps = first.reps,
                        weightGrams = exercise.sets.maxOf { it.weightGrams },
                    )
                }
            },
    )
