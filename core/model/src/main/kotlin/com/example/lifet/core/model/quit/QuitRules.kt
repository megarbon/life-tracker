package com.example.lifet.core.model.quit

/**
 * Constantes de negocio de Dejar de fumar y beber. Los límites de validación son los que acepta el formulario; los
 * datos de demo también los respetan.
 */
public object QuitRules {
    public const val SECONDS_PER_DAY: Long = 86_400L
    public const val SECONDS_PER_WEEK: Long = 604_800L
    public const val HOURS_PER_DAY: Int = 24
    public const val DAYS_PER_WEEK: Int = 7

    public const val MIN_INTENSITY: Int = 1
    public const val MAX_INTENSITY: Int = 5

    /** Meses que cubre el calendario de días limpios. */
    public const val HEATMAP_MONTHS: Int = 3

    /** Intensidad con la que el calendario pinta un día con consumo: más tenue que un día limpio, nunca vacío. */
    public const val CONSUMPTION_DAY_LEVEL: Float = 0.25f

    /** Puntos que dibuja la gráfica de ahorro acumulado. */
    public const val SAVINGS_POINTS: Int = 12

    /** Semanas completas que cuentan para "cumplido X de 4 semanas" en el modo reducción. */
    public const val COMPLIANCE_WEEKS: Int = 4

    /** Semanas que muestra la gráfica de consumo semanal (incluida la actual). */
    public const val WEEKS_SHOWN: Int = 8

    /** Combinaciones recientes de disparador y estrategia que se ofrecen al registrar un antojo. */
    public const val RECENT_CRAVING_COMBOS: Int = 5

    /** Horas desde que se alcanza un hito en las que se destaca como "nuevo". */
    public const val NEW_MILESTONE_HOURS: Long = 48L

    public const val DEFAULT_CIGARETTES_PER_PACK: Int = 20

    // Límites de validación.
    public const val MAX_BASELINE_UNITS: Int = 1_000
    public const val MAX_UNITS_PER_PACK: Int = 1_000
    public const val MAX_PACK_PRICE_MINOR: Long = 100_000_000_000L
    public const val MAX_WEEKLY_LIMIT: Int = 1_000
    public const val MAX_CONSUMPTION_UNITS: Int = 1_000
    public const val NAME_MAX_LENGTH: Int = 60
    public const val NOTE_MAX_LENGTH: Int = 2_000
}
