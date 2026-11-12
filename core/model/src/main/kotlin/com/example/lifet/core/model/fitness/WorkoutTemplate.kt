package com.example.lifet.core.model.fitness

/** Ejercicio de una plantilla: [sets] series de [reps] repeticiones con [weightGrams]. */
public data class TemplateExercise(
    public val name: String,
    public val sets: Int,
    public val reps: Int,
    public val weightGrams: Long,
)

/** Plantilla de rutina para repetir un entreno en un toque. [durationSec] es la duración habitual. */
public data class WorkoutTemplate(
    public val id: String,
    public val name: String,
    public val type: WorkoutType,
    public val durationSec: Long,
    public val exercises: List<TemplateExercise> = emptyList(),
)
