package com.example.lifet.core.database.nutrition

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
public interface NutritionMealDao {
    @Query("SELECT * FROM nutrition_meal WHERE date = :date ORDER BY loggedAt")
    public fun observeByDate(date: LocalDate): Flow<List<NutritionMealEntity>>

    @Query("SELECT * FROM nutrition_meal WHERE date >= :start AND date < :endExclusive ORDER BY date, loggedAt")
    public fun observeBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<NutritionMealEntity>>

    @Query("SELECT * FROM nutrition_meal WHERE date = :date ORDER BY loggedAt")
    public suspend fun getByDate(date: LocalDate): List<NutritionMealEntity>

    /** Alimentos usados recientemente, sin repetir, del más reciente al más antiguo. */
    @Query("SELECT foodId FROM nutrition_meal GROUP BY foodId ORDER BY MAX(loggedAt) DESC LIMIT :limit")
    public fun observeRecentFoodIds(limit: Int): Flow<List<String>>

    @Upsert
    public suspend fun upsert(entry: NutritionMealEntity)

    @Upsert
    public suspend fun upsertAll(entries: List<NutritionMealEntity>)

    @Query("DELETE FROM nutrition_meal WHERE id = :id")
    public suspend fun deleteById(id: String)

    @Query("DELETE FROM nutrition_meal")
    public suspend fun deleteAll()
}
