package com.example.lifet.core.model.fitness

/**
 * Constantes de negocio de Deporte. Todo se guarda en SI y en enteros: segundos, metros y gramos.
 * Los límites de validación son los que acepta el formulario; los datos de demo también los respetan.
 */
public object FitnessRules {
    /** Referencia de la OMS: 150 minutos de actividad moderada a la semana. */
    public const val WEEKLY_GOAL_MINUTES: Int = 150

    /** Objetivo de pasos por defecto, hasta que Ajustes permita cambiarlo. */
    public const val DEFAULT_STEPS_GOAL: Int = 8_000

    public const val SECONDS_PER_MINUTE: Long = 60L
    public const val GRAMS_PER_KILO: Long = 1_000L
    public const val METERS_PER_KILOMETER: Long = 1_000L

    /** Semanas que muestra la gráfica de minutos. */
    public const val CHART_WEEKS: Int = 12

    /** Ventana de la media móvil del peso. */
    public const val MOVING_AVERAGE_DAYS: Int = 7

    /** Ventana con la que se calcula el cambio de peso. */
    public const val WEIGHT_TREND_DAYS: Int = 30

    /** Minutos de un día que pintan la casilla del heatmap con la máxima intensidad. */
    public const val HEATMAP_FULL_MINUTES: Int = 60

    /** Casilla más tenue del heatmap con actividad: un día con 5 minutos debe distinguirse de uno sin nada. */
    public const val HEATMAP_MIN_INTENSITY: Float = 0.1f

    /** Divisor de la fórmula de Epley: 1RM = carga × (1 + repeticiones / 30). */
    public const val ONE_REP_MAX_DIVISOR: Long = 30L

    public const val RECENT_RECORDS: Int = 5
    public const val RECENT_WORKOUTS: Int = 6

    // Límites de validación.
    public const val MAX_DURATION_SECONDS: Long = 86_400L
    public const val MAX_DISTANCE_METERS: Long = 1_000_000L
    public const val MAX_ENERGY_KCAL: Int = 100_000
    public const val MAX_EXERCISES: Int = 30
    public const val MAX_SETS_PER_EXERCISE: Int = 50
    public const val MAX_REPS: Int = 1_000
    public const val MAX_SET_WEIGHT_GRAMS: Long = 1_000_000_000L
    public const val MIN_BODY_WEIGHT_GRAMS: Long = 20_000L
    public const val MAX_BODY_WEIGHT_GRAMS: Long = 500_000L
    public const val MAX_STEPS: Int = 200_000
    public const val TITLE_MAX_LENGTH: Int = 120
    public const val EXERCISE_NAME_MAX_LENGTH: Int = 80
    public const val NOTE_MAX_LENGTH: Int = 2_000
}
