package com.example.lifet.core.database.nutrition

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Alimento registrado en una comida. Guarda nombre y nutrientes ya calculados: el historial no cambia si luego
 * se edita o se borra el alimento de la base.
 */
@Entity(
    tableName = "nutrition_meal",
    indices = [Index(value = ["date"]), Index(value = ["loggedAt"])],
)
public data class NutritionMealEntity(
    @PrimaryKey public val id: String,
    public val date: LocalDate,
    public val mealType: String,
    public val foodId: String,
    public val foodName: String,
    public val grams: Int,
    public val kcal: Double,
    public val proteinG: Double,
    public val carbsG: Double,
    public val fatG: Double,
    public val fiberG: Double,
    public val sugarG: Double,
    public val loggedAt: Instant,
    public val zone: ZoneId,
)
