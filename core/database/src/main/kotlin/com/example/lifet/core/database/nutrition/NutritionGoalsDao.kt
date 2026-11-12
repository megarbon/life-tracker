package com.example.lifet.core.database.nutrition

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
public interface NutritionGoalsDao {
    @Query("SELECT * FROM nutrition_goals WHERE id = ${NutritionGoalsEntity.SINGLETON_ID}")
    public fun observe(): Flow<NutritionGoalsEntity?>

    @Upsert
    public suspend fun upsert(goals: NutritionGoalsEntity)

    @Query("DELETE FROM nutrition_goals")
    public suspend fun deleteAll()
}
