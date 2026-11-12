package com.example.lifet.core.model.mood

/** Constantes de negocio del área de ánimo. */
public object MoodRules {
    /** Escala de energía y estrés. */
    public const val SCALE_MIN: Int = 1
    public const val SCALE_MAX: Int = 5

    /** Longitud máxima de la nota y de la gratitud. */
    public const val TEXT_MAX_LENGTH: Int = 2_000

    /** Máximo de emociones o factores por entrada. */
    public const val MAX_TAGS: Int = 20

    /** Ventana de la media móvil. */
    public const val MOVING_AVERAGE_DAYS: Int = 7

    /** Un día cuenta como "bajo" si su ánimo medio es menor que este valor. */
    public const val LOW_DAY_THRESHOLD: Double = 2.5

    /** Días bajos seguidos a partir de los cuales se ofrecen recursos de apoyo. */
    public const val LOW_STREAK_DAYS: Int = 5

    /** Días con y sin un factor necesarios para comparar su impacto. */
    public const val MIN_FACTOR_DAYS: Int = 3

    /** Cuántos días mira el resumen de hoy hacia atrás (media semanal y racha). */
    public const val SUMMARY_WINDOW_DAYS: Int = 14
}
