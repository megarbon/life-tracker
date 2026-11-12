package com.example.lifet.core.model.nutrition

/**
 * Objetivos diarios definidos por el usuario (nunca impuestos por la app): energía, macros en gramos y agua.
 * Son orientativos, sin lenguaje de dieta restrictiva.
 */
public data class NutritionGoals(
    public val kcal: Int,
    public val proteinG: Int,
    public val carbsG: Int,
    public val fatG: Int,
    public val waterMl: Int,
) {
    /** `true` si todos los valores están en rangos razonables. */
    public val isValid: Boolean
        get() =
            kcal in KCAL_RANGE &&
                proteinG in MACRO_RANGE &&
                carbsG in MACRO_RANGE &&
                fatG in MACRO_RANGE &&
                waterMl in WATER_RANGE

    /** Energía que sumarían los macros objetivo; permite avisar si no cuadra con [kcal]. */
    public val macroKcal: Int
        get() =
            (
                proteinG * Nutrients.KCAL_PER_G_PROTEIN +
                    carbsG * Nutrients.KCAL_PER_G_CARBS +
                    fatG * Nutrients.KCAL_PER_G_FAT
            ).toInt()

    public companion object {
        public val KCAL_RANGE: IntRange = 500..10_000
        public val MACRO_RANGE: IntRange = 0..1_000
        public val WATER_RANGE: IntRange = 0..10_000

        public val DEFAULT: NutritionGoals =
            NutritionGoals(kcal = 2_000, proteinG = 100, carbsG = 250, fatG = 65, waterMl = 2_000)
    }
}
