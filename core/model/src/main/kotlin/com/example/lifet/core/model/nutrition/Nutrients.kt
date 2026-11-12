package com.example.lifet.core.model.nutrition

/**
 * Valores nutricionales. Se usa tanto "por 100 g" (en [FoodItem]) como "totales" (en [MealEntry] y [DayTotals]).
 * Energía en kcal y el resto en gramos; `Double` porque no es dinero y los totales se acumulan con fracciones.
 */
public data class Nutrients(
    public val kcal: Double,
    public val proteinG: Double,
    public val carbsG: Double,
    public val fatG: Double,
    public val fiberG: Double = 0.0,
    public val sugarG: Double = 0.0,
) {
    public operator fun plus(other: Nutrients): Nutrients =
        Nutrients(
            kcal = kcal + other.kcal,
            proteinG = proteinG + other.proteinG,
            carbsG = carbsG + other.carbsG,
            fatG = fatG + other.fatG,
            fiberG = fiberG + other.fiberG,
            sugarG = sugarG + other.sugarG,
        )

    public operator fun times(factor: Double): Nutrients =
        Nutrients(
            kcal = kcal * factor,
            proteinG = proteinG * factor,
            carbsG = carbsG * factor,
            fatG = fatG * factor,
            fiberG = fiberG * factor,
            sugarG = sugarG * factor,
        )

    /** Reparto de la energía de los macros (proteína, hidratos, grasa), o [MacroSplit.ZERO] si no hay macros. */
    public fun macroSplit(): MacroSplit {
        val protein = proteinG * KCAL_PER_G_PROTEIN
        val carbs = carbsG * KCAL_PER_G_CARBS
        val fat = fatG * KCAL_PER_G_FAT
        val total = protein + carbs + fat
        return if (total <= 0.0) MacroSplit.ZERO else MacroSplit(protein / total, carbs / total, fat / total)
    }

    public companion object {
        public val ZERO: Nutrients = Nutrients(kcal = 0.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0)

        /** Equivalencias de Atwater, usadas para repartir la energía entre macros. */
        public const val KCAL_PER_G_PROTEIN: Double = 4.0
        public const val KCAL_PER_G_CARBS: Double = 4.0
        public const val KCAL_PER_G_FAT: Double = 9.0
    }
}
