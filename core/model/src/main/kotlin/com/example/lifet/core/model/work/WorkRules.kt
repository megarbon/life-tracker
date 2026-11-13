package com.example.lifet.core.model.work

/**
 * Constantes de negocio de Trabajo. Todo el tiempo se guarda como `Instant` + `ZoneId` y las duraciones en segundos
 * enteros. Los límites de validación son los que acepta el formulario; los datos de demo también los respetan.
 */
public object WorkRules {
    /** Objetivo semanal por defecto: 40 horas, hasta que el usuario lo cambie. */
    public const val DEFAULT_WEEKLY_TARGET_MINUTES: Int = 2_400
    public const val MAX_WEEKLY_TARGET_MINUTES: Int = 10_080

    public const val SECONDS_PER_MINUTE: Long = 60L
    public const val MINUTES_PER_HOUR: Int = 60
    public const val SECONDS_PER_HOUR: Long = 3_600L

    /** Una jornada no puede durar más de 24 horas. */
    public const val MAX_SHIFT_SECONDS: Long = 86_400L

    /**
     * Una jornada abierta más tiempo que esto se considera olvidada: no cuenta en los totales hasta que se cierre
     * (no se inventan horas) y la pantalla invita a indicar a qué hora terminó.
     */
    public const val FORGOTTEN_AFTER_SECONDS: Long = 57_600L

    public const val MAX_BREAKS: Int = 20
    public const val MAX_PROJECTS: Int = 50

    // Pomodoro.
    public const val DEFAULT_FOCUS_MINUTES: Int = 25
    public const val SHORT_BREAK_MINUTES: Int = 5
    public const val LONG_BREAK_MINUTES: Int = 15
    public const val FOCUS_ROUNDS_BEFORE_LONG_BREAK: Int = 4
    public const val MAX_FOCUS_MINUTES: Int = 240

    /** Una sesión terminada antes de este tiempo se descarta en lugar de guardarse. */
    public const val MIN_RECORDED_FOCUS_SECONDS: Long = 60L

    /** Semanas que muestra la gráfica de saldo acumulado. */
    public const val CHART_WEEKS: Int = 12
    public const val RECENT_FOCUS_SESSIONS: Int = 8
    public const val RECENT_SHIFT_SUGGESTIONS: Int = 4

    // Límites de texto.
    public const val PROJECT_NAME_MAX_LENGTH: Int = 60
    public const val SHIFT_NOTE_MAX_LENGTH: Int = 500
    public const val DAY_NOTE_MAX_LENGTH: Int = 2_000
    public const val FOCUS_TITLE_MAX_LENGTH: Int = 120
}
