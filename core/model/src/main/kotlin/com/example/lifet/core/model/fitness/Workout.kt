package com.example.lifet.core.model.fitness

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Una serie de fuerza: [reps] repeticiones con [weightGrams] (0 = peso corporal). */
public data class StrengthSet(
    public val reps: Int,
    public val weightGrams: Long,
) {
    public val isBodyweight: Boolean get() = weightGrams == 0L

    public val volumeGrams: Long get() = weightGrams * reps

    /**
     * 1RM estimado con la fórmula de Epley (carga × (1 + reps / 30)). Con una sola repetición es la propia carga.
     * Es `null` con peso corporal: ahí el récord se mide en repeticiones.
     */
    public val estimatedOneRepMaxGrams: Long?
        get() =
            when {
                weightGrams <= 0L -> null
                reps <= 1 -> weightGrams
                else -> weightGrams * (FitnessRules.ONE_REP_MAX_DIVISOR + reps) / FitnessRules.ONE_REP_MAX_DIVISOR
            }
}

/** Un ejercicio dentro de un entreno de fuerza, con sus series en orden. */
public data class ExerciseLog(
    public val name: String,
    public val sets: List<StrengthSet>,
) {
    public val volumeGrams: Long get() = sets.sumOf { it.volumeGrams }
}

/**
 * Un entreno. Se guarda el instante de inicio (UTC) con la [zone] del usuario en ese momento, de modo que el
 * día lógico ([startDate]) no cambia si viaja. [distanceM] y [energyKcal] sólo tienen sentido en cardio;
 * [exercises] sólo en fuerza. [externalId] sirve para deduplicar cuando llegue Health Connect.
 */
public data class Workout(
    public val id: String,
    public val type: WorkoutType,
    public val title: String,
    public val start: Instant,
    public val zone: ZoneId,
    public val durationSec: Long,
    public val distanceM: Long? = null,
    public val energyKcal: Int? = null,
    public val exercises: List<ExerciseLog> = emptyList(),
    public val note: String = "",
    public val source: RecordSource = RecordSource.MANUAL,
    public val externalId: String? = null,
) {
    /** Día lógico en el que empezó. */
    public val startDate: LocalDate get() = start.atZone(zone).toLocalDate()

    public val end: Instant get() = start.plusSeconds(durationSec.coerceAtLeast(0L))

    public val totalSets: Int get() = exercises.sumOf { it.sets.size }

    public val volumeGrams: Long get() = exercises.sumOf { it.volumeGrams }

    /**
     * Segundos de actividad de cada día lógico. Un entreno que cruza la medianoche se reparte entre los días que
     * toca, y se cuenta con instantes reales, así que un cambio de hora no desajusta el reparto.
     */
    public fun secondsByDay(): Map<LocalDate, Long> {
        val result = LinkedHashMap<LocalDate, Long>()
        val finish = end
        var cursor = start
        while (cursor < finish) {
            val date = cursor.atZone(zone).toLocalDate()
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant()
            val sliceEnd = if (nextMidnight < finish) nextMidnight else finish
            result.merge(date, Duration.between(cursor, sliceEnd).seconds, Long::plus)
            cursor = sliceEnd
        }
        return result
    }
}
