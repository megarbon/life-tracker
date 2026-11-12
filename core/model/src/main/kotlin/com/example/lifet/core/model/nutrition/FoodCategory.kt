package com.example.lifet.core.model.nutrition

/** Familia de alimento; sirve para etiquetar y agrupar, no para juzgar. */
public enum class FoodCategory {
    FRUIT,
    VEGETABLE,
    LEGUME,
    GRAIN,
    MEAT,
    FISH,
    DAIRY_EGGS,
    FATS_NUTS,
    SWEETS_SNACKS,
    DISHES,
    DRINKS,
    OTHER,
    ;

    public companion object {
        public fun fromName(name: String?): FoodCategory = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
