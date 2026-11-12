package com.example.lifet.core.database.nutrition

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Objetivos del usuario: una sola fila con [id] = [SINGLETON_ID]; si falta, rigen los valores por defecto. */
@Entity(tableName = "nutrition_goals")
public data class NutritionGoalsEntity(
    @PrimaryKey public val id: Int = SINGLETON_ID,
    public val kcal: Int,
    public val proteinG: Int,
    public val carbsG: Int,
    public val fatG: Int,
    public val waterMl: Int,
) {
    public companion object {
        public const val SINGLETON_ID: Int = 1
    }
}
