package com.example.lifet.core.database.nutrition

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Alimento de la base local. Los valores son por 100 g. Las filas de la semilla tienen [isCustom] = `false`. */
@Entity(tableName = "nutrition_food")
public data class NutritionFoodEntity(
    @PrimaryKey public val id: String,
    public val name: String,
    public val category: String,
    public val kcalPer100g: Double,
    public val proteinPer100g: Double,
    public val carbsPer100g: Double,
    public val fatPer100g: Double,
    public val fiberPer100g: Double,
    public val sugarPer100g: Double,
    public val servingGrams: Int,
    public val isCustom: Boolean,
    public val isFavorite: Boolean,
)
