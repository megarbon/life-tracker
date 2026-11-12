package com.example.lifet.core.data.nutrition

import com.example.lifet.core.database.nutrition.NutritionFoodEntity
import com.example.lifet.core.database.nutrition.NutritionGoalsEntity
import com.example.lifet.core.database.nutrition.NutritionMealEntity
import com.example.lifet.core.database.nutrition.NutritionWaterEntity
import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry

internal fun NutritionFoodEntity.toDomain(): FoodItem =
    FoodItem(
        id = id,
        name = name,
        category = FoodCategory.fromName(category),
        per100g =
            Nutrients(
                kcal = kcalPer100g,
                proteinG = proteinPer100g,
                carbsG = carbsPer100g,
                fatG = fatPer100g,
                fiberG = fiberPer100g,
                sugarG = sugarPer100g,
            ),
        servingGrams = servingGrams,
        isCustom = isCustom,
        isFavorite = isFavorite,
    )

internal fun FoodItem.toEntity(): NutritionFoodEntity =
    NutritionFoodEntity(
        id = id,
        name = name,
        category = category.name,
        kcalPer100g = per100g.kcal,
        proteinPer100g = per100g.proteinG,
        carbsPer100g = per100g.carbsG,
        fatPer100g = per100g.fatG,
        fiberPer100g = per100g.fiberG,
        sugarPer100g = per100g.sugarG,
        servingGrams = servingGrams,
        isCustom = isCustom,
        isFavorite = isFavorite,
    )

/** Una fila con un tipo de comida desconocido (p. ej. de una versión futura) se trata como snack, no se pierde. */
internal fun NutritionMealEntity.toDomain(): MealEntry =
    MealEntry(
        id = id,
        date = date,
        mealType = MealType.fromName(mealType) ?: MealType.SNACK,
        foodId = foodId,
        foodName = foodName,
        grams = grams,
        nutrients = Nutrients(kcal, proteinG, carbsG, fatG, fiberG, sugarG),
        loggedAt = loggedAt,
        zone = zone,
    )

internal fun MealEntry.toEntity(): NutritionMealEntity =
    NutritionMealEntity(
        id = id,
        date = date,
        mealType = mealType.name,
        foodId = foodId,
        foodName = foodName,
        grams = grams,
        kcal = nutrients.kcal,
        proteinG = nutrients.proteinG,
        carbsG = nutrients.carbsG,
        fatG = nutrients.fatG,
        fiberG = nutrients.fiberG,
        sugarG = nutrients.sugarG,
        loggedAt = loggedAt,
        zone = zone,
    )

internal fun NutritionWaterEntity.toDomain(): WaterEntry = WaterEntry(id, date, ml, loggedAt, zone)

internal fun WaterEntry.toEntity(): NutritionWaterEntity = NutritionWaterEntity(id, date, ml, loggedAt, zone)

internal fun NutritionGoalsEntity.toDomain(): NutritionGoals = NutritionGoals(kcal, proteinG, carbsG, fatG, waterMl)

internal fun NutritionGoals.toEntity(): NutritionGoalsEntity =
    NutritionGoalsEntity(kcal = kcal, proteinG = proteinG, carbsG = carbsG, fatG = fatG, waterMl = waterMl)
