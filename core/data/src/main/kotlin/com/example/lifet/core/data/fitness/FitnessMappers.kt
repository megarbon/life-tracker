@file:Suppress("TooManyFunctions") // Un par de mapeos entity <-> dominio por cada tabla.

package com.example.lifet.core.data.fitness

import com.example.lifet.core.database.fitness.FitnessSetEntity
import com.example.lifet.core.database.fitness.FitnessStepsEntity
import com.example.lifet.core.database.fitness.FitnessTemplateEntity
import com.example.lifet.core.database.fitness.FitnessTemplateExerciseEntity
import com.example.lifet.core.database.fitness.FitnessWeightEntity
import com.example.lifet.core.database.fitness.FitnessWorkoutEntity
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate

/** Entreno sin series: las series se leen de otra tabla y se unen en [toDomain]. */
internal fun FitnessWorkoutEntity.toDomain(exercises: List<ExerciseLog>): Workout =
    Workout(
        id = id,
        type = type,
        title = title,
        start = start,
        zone = zoneId,
        durationSec = durationSec,
        distanceM = distanceM,
        energyKcal = energyKcal,
        exercises = exercises,
        note = note,
        source = source,
        externalId = externalId,
    )

internal fun Workout.toEntity(): FitnessWorkoutEntity =
    FitnessWorkoutEntity(
        id = id,
        type = type,
        title = title,
        start = start,
        zoneId = zone,
        durationSec = durationSec,
        distanceM = distanceM,
        energyKcal = energyKcal,
        note = note,
        source = source,
        externalId = externalId,
    )

/** Aplana los ejercicios en filas, conservando el orden de ejercicios y de series. */
internal fun Workout.toSetEntities(): List<FitnessSetEntity> =
    exercises.flatMapIndexed { exerciseIndex, exercise ->
        exercise.sets.mapIndexed { setIndex, set ->
            FitnessSetEntity(
                workoutId = id,
                exerciseIndex = exerciseIndex,
                exerciseName = exercise.name,
                setIndex = setIndex,
                reps = set.reps,
                weightGrams = set.weightGrams,
            )
        }
    }

/** Reagrupa las filas de series (ya ordenadas por entreno, ejercicio y serie) en ejercicios. */
internal fun List<FitnessSetEntity>.toExerciseLogs(): List<ExerciseLog> =
    groupBy { it.exerciseIndex }
        .toSortedMap()
        .values
        .map { rows ->
            ExerciseLog(
                rows.first().exerciseName,
                rows.sortedBy { it.setIndex }.map { StrengthSet(it.reps, it.weightGrams) },
            )
        }

internal fun FitnessWeightEntity.toDomain(): BodyWeight = BodyWeight(date, weightGrams, note)

internal fun BodyWeight.toEntity(): FitnessWeightEntity = FitnessWeightEntity(date, weightGrams, note)

internal fun FitnessStepsEntity.toDomain(): DailySteps = DailySteps(date, steps, source)

internal fun DailySteps.toEntity(): FitnessStepsEntity = FitnessStepsEntity(date, steps, source)

internal fun FitnessTemplateEntity.toDomain(exercises: List<FitnessTemplateExerciseEntity>): WorkoutTemplate =
    WorkoutTemplate(
        id = id,
        name = name,
        type = type,
        durationSec = durationSec,
        exercises =
            exercises.sortedBy { it.position }.map {
                TemplateExercise(
                    it.name,
                    it.sets,
                    it.reps,
                    it.weightGrams,
                )
            },
    )

internal fun WorkoutTemplate.toEntity(): FitnessTemplateEntity =
    FitnessTemplateEntity(id = id, name = name, type = type, durationSec = durationSec)

internal fun WorkoutTemplate.toExerciseEntities(): List<FitnessTemplateExerciseEntity> =
    exercises.mapIndexed { position, exercise ->
        FitnessTemplateExerciseEntity(
            templateId = id,
            position = position,
            name = exercise.name,
            sets = exercise.sets,
            reps = exercise.reps,
            weightGrams = exercise.weightGrams,
        )
    }
