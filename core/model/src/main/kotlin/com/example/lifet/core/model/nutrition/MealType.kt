package com.example.lifet.core.model.nutrition

import java.time.LocalTime

/** Momento del día al que pertenece un alimento registrado. El orden de declaración es el orden en pantalla. */
public enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    ;

    public companion object {
        private const val BREAKFAST_UNTIL_HOUR = 11
        private const val LUNCH_UNTIL_HOUR = 16
        private const val SNACK_UNTIL_HOUR = 19

        /** Comida más probable a esa hora, para preseleccionarla al añadir. */
        public fun forTime(time: LocalTime): MealType =
            when {
                time.hour < BREAKFAST_UNTIL_HOUR -> BREAKFAST
                time.hour < LUNCH_UNTIL_HOUR -> LUNCH
                time.hour < SNACK_UNTIL_HOUR -> SNACK
                else -> DINNER
            }

        /** Convierte el nombre guardado en rutas o BD; `null` si no es válido. */
        public fun fromName(name: String?): MealType? = entries.firstOrNull { it.name == name }
    }
}
