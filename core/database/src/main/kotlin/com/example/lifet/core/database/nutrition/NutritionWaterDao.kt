package com.example.lifet.core.database.nutrition

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
public interface NutritionWaterDao {
    @Query("SELECT * FROM nutrition_water WHERE date = :date ORDER BY loggedAt")
    public fun observeByDate(date: LocalDate): Flow<List<NutritionWaterEntity>>

    @Query("SELECT * FROM nutrition_water WHERE date >= :start AND date < :endExclusive ORDER BY date, loggedAt")
    public fun observeBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<NutritionWaterEntity>>

    @Query("SELECT * FROM nutrition_water WHERE date = :date ORDER BY loggedAt")
    public suspend fun getByDate(date: LocalDate): List<NutritionWaterEntity>

    @Upsert
    public suspend fun upsert(entry: NutritionWaterEntity)

    @Upsert
    public suspend fun upsertAll(entries: List<NutritionWaterEntity>)

    @Query("DELETE FROM nutrition_water WHERE id = :id")
    public suspend fun deleteById(id: String)

    @Query("DELETE FROM nutrition_water")
    public suspend fun deleteAll()
}
