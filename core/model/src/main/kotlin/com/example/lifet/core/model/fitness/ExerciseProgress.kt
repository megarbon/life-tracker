package com.example.lifet.core.model.fitness

import java.time.LocalDate
import java.util.Locale

/** Tipo de récord personal. Con carga se mide el 1RM estimado; con peso corporal, las repeticiones de una serie. */
public enum class RecordKind { ONE_REP_MAX, BODYWEIGHT_REPS }

/**
 * Un récord batido. [value] está en gramos para [RecordKind.ONE_REP_MAX] y en repeticiones para
 * [RecordKind.BODYWEIGHT_REPS]. La primera vez que se hace un ejercicio no cuenta como récord: no hay marca previa.
 */
public data class PersonalRecord(
    public val exerciseName: String,
    public val kind: RecordKind,
    public val value: Long,
    public val date: LocalDate,
    public val workoutId: String,
)

/** Lo que se hizo de un ejercicio en un entreno. */
public data class ExerciseSession(
    public val date: LocalDate,
    public val workoutId: String,
    public val sets: List<StrengthSet>,
) {
    public val topWeightGrams: Long get() = sets.maxOfOrNull { it.weightGrams } ?: 0L

    /** Mejor 1RM estimado entre las series con carga, o `null` si todas fueron con peso corporal. */
    public val bestOneRepMaxGrams: Long? get() = sets.mapNotNull { it.estimatedOneRepMaxGrams }.maxOrNull()

    /** Máximo de repeticiones en una serie con peso corporal, o `null` si no hubo ninguna. */
    public val bestBodyweightReps: Int? get() = sets.filter { it.isBodyweight }.maxOfOrNull { it.reps }

    public val volumeGrams: Long get() = sets.sumOf { it.volumeGrams }
}

/** Historial de un ejercicio. [sessions] va en orden cronológico. */
public data class ExerciseProgress(
    public val key: String,
    public val name: String,
    public val sessions: List<ExerciseSession>,
    public val records: List<PersonalRecord>,
) {
    public val lastDate: LocalDate? get() = sessions.lastOrNull()?.date

    public val bestOneRepMaxGrams: Long? get() = sessions.mapNotNull { it.bestOneRepMaxGrams }.maxOrNull()

    public val bestReps: Int? get() = sessions.mapNotNull { it.bestBodyweightReps }.maxOrNull()

    /** `true` si nunca se ha usado carga: la progresión se mide en repeticiones. */
    public val isBodyweightOnly: Boolean get() = sessions.none { it.bestOneRepMaxGrams != null }
}

/** Historial de todos los ejercicios y los récords batidos, del más antiguo al más reciente. */
public data class ExerciseHistory(
    public val exercises: List<ExerciseProgress>,
    public val records: List<PersonalRecord>,
) {
    public val recordsByWorkout: Map<String, List<PersonalRecord>> get() = records.groupBy { it.workoutId }
}

/** Cálculos puros sobre los ejercicios de fuerza: progresión por ejercicio y récords automáticos. */
public object ExerciseAnalytics {
    private val WHITESPACE = Regex("""\s+""")

    /** Clave estable de un ejercicio: "Press  Banca" y "press banca" son el mismo. */
    public fun key(name: String): String = name.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")

    /**
     * Recorre los entrenos en orden cronológico, agrupa las series por ejercicio y detecta los récords: una sesión
     * bate el récord si supera la mejor marca anterior del mismo tipo. El nombre mostrado es la grafía más reciente.
     */
    public fun history(workouts: List<Workout>): ExerciseHistory {
        val tracker = RecordTracker()
        workouts.sortedBy { it.start }.forEach(tracker::add)
        return tracker.build()
    }

    /** Acumula sesiones y marcas máximas mientras se recorren los entrenos en orden. */
    private class RecordTracker {
        private val sessions = LinkedHashMap<String, MutableList<ExerciseSession>>()
        private val names = HashMap<String, String>()
        private val records = mutableListOf<PersonalRecord>()
        private val bestOneRepMax = HashMap<String, Long>()
        private val bestReps = HashMap<String, Int>()

        fun add(workout: Workout) {
            workout.exercises.groupBy { key(it.name) }.forEach { (key, logs) -> addExercise(workout, key, logs) }
        }

        private fun addExercise(
            workout: Workout,
            key: String,
            logs: List<ExerciseLog>,
        ) {
            val sets = logs.flatMap { it.sets }.filter { it.reps > 0 }
            if (key.isEmpty() || sets.isEmpty()) return
            val session = ExerciseSession(workout.startDate, workout.id, sets)
            val name = logs.last().name.trim()
            sessions.getOrPut(key) { mutableListOf() } += session
            names[key] = name
            session.bestOneRepMaxGrams?.let { value ->
                if (bestOneRepMax.raise(key, value)) {
                    records += PersonalRecord(name, RecordKind.ONE_REP_MAX, value, session.date, workout.id)
                }
            }
            session.bestBodyweightReps?.let { reps ->
                if (bestReps.raise(key, reps)) {
                    records += PersonalRecord(name, RecordKind.BODYWEIGHT_REPS, reps.toLong(), session.date, workout.id)
                }
            }
        }

        fun build(): ExerciseHistory {
            val recordsByKey = records.groupBy { key(it.exerciseName) }
            val exercises =
                sessions.map { (key, list) ->
                    ExerciseProgress(key, names.getValue(key), list, recordsByKey[key].orEmpty())
                }
            return ExerciseHistory(exercises, records.toList())
        }

        /** Anota [value] como nueva mejor marca de [key]; devuelve `true` si superó una marca anterior. */
        private fun <V : Comparable<V>> MutableMap<String, V>.raise(
            key: String,
            value: V,
        ): Boolean {
            val previous = this[key]
            if (previous == null || value > previous) this[key] = value
            return previous != null && value > previous
        }
    }
}
