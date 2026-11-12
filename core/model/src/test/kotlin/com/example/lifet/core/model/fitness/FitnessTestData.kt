package com.example.lifet.core.model.fitness

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal val MADRID: ZoneId = ZoneId.of("Europe/Madrid")

/** Miércoles. La semana en curso va del lunes 9 al domingo 15 de marzo de 2026. */
internal val TODAY: LocalDate = LocalDate.of(2026, 3, 11)

internal fun cardio(
    id: String,
    date: LocalDate,
    minutes: Long,
    type: WorkoutType = WorkoutType.RUN,
    time: LocalTime = LocalTime.of(18, 0),
    title: String = "",
): Workout =
    Workout(
        id = id,
        type = type,
        title = title,
        start = date.atTime(time).atZone(MADRID).toInstant(),
        zone = MADRID,
        durationSec = minutes * 60,
    )

internal fun strength(
    id: String,
    date: LocalDate,
    vararg exercises: ExerciseLog,
    minutes: Long = 60,
): Workout =
    Workout(
        id = id,
        type = WorkoutType.STRENGTH,
        title = "Fuerza",
        start = date.atTime(LocalTime.of(19, 0)).atZone(MADRID).toInstant(),
        zone = MADRID,
        durationSec = minutes * 60,
        exercises = exercises.toList(),
    )

internal fun lift(
    name: String,
    vararg sets: Pair<Int, Long>,
): ExerciseLog = ExerciseLog(name, sets.map { (reps, grams) -> StrengthSet(reps, grams) })
