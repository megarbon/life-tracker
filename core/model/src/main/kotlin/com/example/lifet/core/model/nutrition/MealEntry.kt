package com.example.lifet.core.model.nutrition

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Un alimento registrado en una comida. Guarda una copia del nombre y de los [nutrients] totales para que el
 * historial no cambie si luego se edita o se borra el alimento. [date] es el día lógico en la zona del usuario;
 * [loggedAt] + [zone] conservan el instante exacto.
 */
public data class MealEntry(
    public val id: String,
    public val date: LocalDate,
    public val mealType: MealType,
    public val foodId: String,
    public val foodName: String,
    public val grams: Int,
    public val nutrients: Nutrients,
    public val loggedAt: Instant,
    public val zone: ZoneId,
) {
    /** Misma entrada con otra cantidad; los nutrientes se reescalan en proporción. */
    public fun withGrams(newGrams: Int): MealEntry =
        if (grams <= 0) {
            this
        } else {
            copy(grams = newGrams, nutrients = nutrients * (newGrams.toDouble() / grams))
        }

    public companion object {
        public const val MIN_GRAMS: Int = 1
        public const val MAX_GRAMS: Int = 5_000
    }
}
