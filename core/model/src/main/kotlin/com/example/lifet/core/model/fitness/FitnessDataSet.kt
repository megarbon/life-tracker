package com.example.lifet.core.model.fitness

/** Todo el contenido de Deporte. El repositorio lo emite entero; la calculadora lo convierte en resúmenes. */
public data class FitnessDataSet(
    public val workouts: List<Workout> = emptyList(),
    public val weights: List<BodyWeight> = emptyList(),
    public val steps: List<DailySteps> = emptyList(),
    public val templates: List<WorkoutTemplate> = emptyList(),
) {
    /** `true` si el área no se ha usado nunca (las plantillas solas no cuentan como uso). */
    public val hasNoRecords: Boolean get() = workouts.isEmpty() && weights.isEmpty() && steps.isEmpty()
}
