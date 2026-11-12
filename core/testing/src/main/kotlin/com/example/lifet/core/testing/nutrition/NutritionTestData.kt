// Datos de ejemplo para tests: los valores nutricionales son datos, no lógica.
@file:Suppress("MagicNumber")

package com.example.lifet.core.testing.nutrition

import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Alimentos y entradas de ejemplo compartidos por los tests de Alimentación. */
public object NutritionTestData {
    public val ZONE: ZoneId = ZoneId.of("Europe/Madrid")

    public val bread: FoodItem =
        FoodItem("seed_pan_blanco", "Pan blanco", FoodCategory.GRAIN, Nutrients(265.0, 9.0, 49.0, 3.2, 2.7, 5.0), 40)

    public val apple: FoodItem =
        FoodItem("seed_manzana", "Manzana", FoodCategory.FRUIT, Nutrients(52.0, 0.3, 14.0, 0.2, 2.4, 10.4), 180)

    public val chicken: FoodItem =
        FoodItem("seed_pechuga_pollo", "Pechuga de pollo", FoodCategory.MEAT, Nutrients(165.0, 31.0, 0.0, 3.6), 150)

    public val oil: FoodItem =
        FoodItem("seed_aceite_oliva", "Aceite de oliva", FoodCategory.FATS_NUTS, Nutrients(884.0, 0.0, 0.0, 100.0), 10)

    public val foods: List<FoodItem> = listOf(bread, apple, chicken, oil)

    public fun entry(
        id: String,
        food: FoodItem,
        grams: Int,
        date: LocalDate,
        mealType: MealType = MealType.BREAKFAST,
        time: LocalTime = LocalTime.of(9, 0),
    ): MealEntry =
        MealEntry(
            id = id,
            date = date,
            mealType = mealType,
            foodId = food.id,
            foodName = food.name,
            grams = grams,
            nutrients = food.nutrientsFor(grams),
            loggedAt = date.atTime(time).atZone(ZONE).toInstant(),
            zone = ZONE,
        )
}
