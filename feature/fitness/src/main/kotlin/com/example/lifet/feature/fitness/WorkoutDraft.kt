package com.example.lifet.feature.fitness

import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.WorkoutType
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

/** Una serie tecleada: repeticiones y carga en kg. Una carga vacía es peso corporal. */
internal data class SetDraft(
    val repsText: String = "",
    val weightText: String = "",
) : Serializable {
    val isBlank: Boolean get() = repsText.isBlank() && weightText.isBlank()

    private companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class ExerciseDraft(
    val name: String = "",
    val sets: List<SetDraft> = listOf(SetDraft()),
) : Serializable {
    val filledSets: List<SetDraft> get() = sets.filterNot { it.isBlank }

    val isBlank: Boolean get() = name.isBlank() && filledSets.isEmpty()

    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Borrador de un entreno (alta y edición). Las operaciones son puras para poder probarlas sin UI.
 *
 * La hora de inicio se guarda como minuto del día. Mientras el usuario no la toque y [autoEndMinute] tenga valor
 * ("ahora"), el inicio se calcula como `ahora - duración`, que es lo natural cuando se registra un entreno recién
 * terminado; así tampoco se cuela en el día siguiente un entreno registrado a última hora.
 */
internal data class WorkoutDraft(
    val id: String,
    val type: WorkoutType = WorkoutType.STRENGTH,
    val title: String = "",
    val date: LocalDate,
    val startMinute: Int = 0,
    val autoEndMinute: Int? = null,
    val durationText: String = "",
    val distanceText: String = "",
    val energyText: String = "",
    val exercises: List<ExerciseDraft> = emptyList(),
    val note: String = "",
    val saveAsTemplate: Boolean = false,
) : Serializable {
    val canAddExercise: Boolean get() = exercises.size < FitnessRules.MAX_EXERCISES

    /** Hora de inicio como hora local. */
    val startTime: LocalTime get() = LocalTime.of(startMinute / MINUTES_IN_HOUR, startMinute % MINUTES_IN_HOUR)

    fun withType(newType: WorkoutType): WorkoutDraft =
        copy(
            type = newType,
            exercises =
                if (newType.isStrength &&
                    exercises.isEmpty()
                ) {
                    listOf(ExerciseDraft())
                } else {
                    exercises
                },
        )

    /** Cambia la duración y, si la hora no se ha tocado, recoloca el inicio para que el entreno acabe "ahora". */
    fun withDuration(text: String): WorkoutDraft {
        val updated = copy(durationText = text)
        val end = autoEndMinute ?: return updated
        val minutes = parseWholeNumber(text) ?: 0
        return updated.copy(startMinute = (end - minutes).coerceIn(0, MINUTES_IN_DAY - 1))
    }

    /** Fija la hora a mano: desde aquí deja de recalcularse sola. */
    fun withStart(minuteOfDay: Int): WorkoutDraft =
        copy(startMinute = minuteOfDay.coerceIn(0, MINUTES_IN_DAY - 1), autoEndMinute = null)

    fun addExercise(): WorkoutDraft = if (canAddExercise) copy(exercises = exercises + ExerciseDraft()) else this

    /** Rellena el primer ejercicio en blanco con [name], o añade uno nuevo si no queda ninguno. */
    fun addExerciseNamed(name: String): WorkoutDraft {
        val blank = exercises.indexOfFirst { it.isBlank }
        return when {
            blank >= 0 -> updateExercise(blank) { it.copy(name = name) }
            canAddExercise -> copy(exercises = exercises + ExerciseDraft(name = name))
            else -> this
        }
    }

    fun removeExercise(index: Int): WorkoutDraft = copy(exercises = exercises.filterIndexed { i, _ -> i != index })

    fun updateExercise(
        index: Int,
        transform: (ExerciseDraft) -> ExerciseDraft,
    ): WorkoutDraft = copy(exercises = exercises.mapIndexed { i, item -> if (i == index) transform(item) else item })

    /** Añade una serie copiando la anterior: lo habitual es repetir carga y repeticiones. */
    fun addSet(exerciseIndex: Int): WorkoutDraft =
        updateExercise(exerciseIndex) { exercise ->
            if (exercise.sets.size >= FitnessRules.MAX_SETS_PER_EXERCISE) {
                exercise
            } else {
                exercise.copy(sets = exercise.sets + (exercise.sets.lastOrNull() ?: SetDraft()))
            }
        }

    fun removeSet(
        exerciseIndex: Int,
        setIndex: Int,
    ): WorkoutDraft = updateExercise(exerciseIndex) { it.copy(sets = it.sets.filterIndexed { i, _ -> i != setIndex }) }

    fun updateSet(
        exerciseIndex: Int,
        setIndex: Int,
        transform: (SetDraft) -> SetDraft,
    ): WorkoutDraft =
        updateExercise(exerciseIndex) { exercise ->
            exercise.copy(sets = exercise.sets.mapIndexed { i, item -> if (i == setIndex) transform(item) else item })
        }

    companion object {
        const val MINUTES_IN_HOUR: Int = 60
        const val MINUTES_IN_DAY: Int = 24 * MINUTES_IN_HOUR
        private const val serialVersionUID = 1L
    }
}
