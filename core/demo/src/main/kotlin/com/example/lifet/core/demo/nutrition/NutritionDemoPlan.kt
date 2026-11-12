package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry

/**
 * Todo lo que un escenario de demo escribe en Room. [goals] `null` significa "sin objetivos propios" (rigen los
 * valores por defecto). La semilla de alimentos no es parte del plan: existe siempre.
 */
internal data class NutritionDemoPlan(
    val meals: List<MealEntry> = emptyList(),
    val water: List<WaterEntry> = emptyList(),
    val goals: NutritionGoals? = null,
    val customFoods: List<FoodItem> = emptyList(),
    val favoriteIds: List<String> = emptyList(),
)
