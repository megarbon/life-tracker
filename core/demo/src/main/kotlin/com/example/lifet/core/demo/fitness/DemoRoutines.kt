@file:Suppress("MagicNumber") // Cargas, series y repeticiones literales: describen una rutina de ejemplo.

package com.example.lifet.core.demo.fitness

/** Granularidad de los discos: las cargas de la demo son múltiplos de 2,5 kg. */
private const val PLATE_GRAMS = 2_500L

/**
 * Un ejercicio de una rutina de demo. La carga sube [weeklyGrams] por cada semana de entrenamiento a partir de
 * [baseGrams]; con [baseGrams] a cero es un ejercicio con peso corporal y progresa en repeticiones.
 */
internal data class ExerciseSpec(
    val name: String,
    val baseGrams: Long,
    val weeklyGrams: Long,
    val sets: Int,
    val reps: Int,
) {
    val isBodyweight: Boolean get() = baseGrams == 0L

    /** Carga de la semana [trainingWeek] al [percent] % (descarga o regreso de vacaciones), a múltiplos de 2,5 kg. */
    fun loadAt(
        trainingWeek: Int,
        percent: Long,
    ): Long {
        if (isBodyweight) return 0L
        val raw = (baseGrams + weeklyGrams * trainingWeek) * percent / 100
        return (raw + PLATE_GRAMS / 2) / PLATE_GRAMS * PLATE_GRAMS
    }
}

/** Las tres sesiones de fuerza de la semana tipo de la demo. */
internal enum class DemoSession(
    val title: String,
    val exercises: List<ExerciseSpec>,
) {
    PUSH(
        "Empuje",
        listOf(
            ExerciseSpec("Press banca", 60_000, 600, 4, 5),
            ExerciseSpec("Press militar", 32_500, 300, 3, 8),
            ExerciseSpec("Fondos", 0, 0, 3, 8),
            ExerciseSpec("Extensión de tríceps en polea", 20_000, 250, 3, 12),
        ),
    ),
    PULL(
        "Tirón",
        listOf(
            ExerciseSpec("Peso muerto", 85_000, 1_000, 3, 5),
            ExerciseSpec("Remo con barra", 52_500, 500, 4, 8),
            ExerciseSpec("Dominadas", 0, 0, 3, 5),
            ExerciseSpec("Curl de bíceps", 15_000, 200, 3, 10),
        ),
    ),
    LEGS(
        "Pierna",
        listOf(
            ExerciseSpec("Sentadilla", 72_500, 1_000, 4, 5),
            ExerciseSpec("Prensa de piernas", 120_000, 1_800, 3, 10),
            ExerciseSpec("Peso muerto rumano", 60_000, 800, 3, 8),
            ExerciseSpec("Elevación de gemelos", 60_000, 600, 4, 12),
        ),
    ),
}
