package com.example.lifet.core.model.fitness

/** Tipo de entreno. [STRENGTH] lleva ejercicios y series; el resto son cardio (duración, distancia y kcal). */
public enum class WorkoutType(
    public val isStrength: Boolean,
) {
    STRENGTH(isStrength = true),
    RUN(isStrength = false),
    WALK(isStrength = false),
    CYCLING(isStrength = false),
    SWIM(isStrength = false),
    HIKE(isStrength = false),
    ROW(isStrength = false),
    OTHER(isStrength = false),
    ;

    public val isCardio: Boolean get() = !isStrength
}
