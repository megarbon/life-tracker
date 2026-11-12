package com.example.lifet.core.model.nutrition

/**
 * Alimento de la base local. [per100g] son los valores por 100 g; [servingGrams] es la ración habitual
 * (una manzana, una cucharada de aceite…). Los de la semilla tienen [isCustom] = `false` y no se pueden borrar.
 */
public data class FoodItem(
    public val id: String,
    public val name: String,
    public val category: FoodCategory,
    public val per100g: Nutrients,
    public val servingGrams: Int,
    public val isCustom: Boolean = false,
    public val isFavorite: Boolean = false,
) {
    /** Aporte de [grams] gramos de este alimento. */
    public fun nutrientsFor(grams: Int): Nutrients = per100g * (grams / GRAMS_BASE)

    private companion object {
        const val GRAMS_BASE = 100.0
    }
}
