package com.example.lifet.core.model.mood

/** Nivel de ánimo en cinco pasos; [value] (1 a 5) es lo que se guarda y se promedia. */
public enum class MoodLevel(
    public val value: Int,
) {
    VERY_LOW(1),
    LOW(2),
    NEUTRAL(3),
    GOOD(4),
    VERY_GOOD(5),
    ;

    public companion object {
        public const val MIN: Int = 1
        public const val MAX: Int = 5

        /** Devuelve el nivel de [value] o `null` si está fuera de 1..5 (dato corrupto o futuro). */
        public fun fromValue(value: Int): MoodLevel? = entries.firstOrNull { it.value == value }
    }
}
