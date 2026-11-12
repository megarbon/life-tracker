package com.example.lifet.core.database.nutrition

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
public interface NutritionFoodDao {
    @Query("SELECT * FROM nutrition_food ORDER BY name COLLATE NOCASE")
    public suspend fun getAll(): List<NutritionFoodEntity>

    @Query(
        "SELECT * FROM nutrition_food WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY isFavorite DESC, name COLLATE NOCASE",
    )
    public fun observeSearch(query: String): Flow<List<NutritionFoodEntity>>

    @Query("SELECT * FROM nutrition_food WHERE id = :id")
    public fun observeById(id: String): Flow<NutritionFoodEntity?>

    @Query("SELECT * FROM nutrition_food WHERE id = :id")
    public suspend fun getById(id: String): NutritionFoodEntity?

    @Query("SELECT * FROM nutrition_food WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE")
    public fun observeFavorites(): Flow<List<NutritionFoodEntity>>

    @Upsert
    public suspend fun upsert(food: NutritionFoodEntity)

    /** Inserta la semilla sin tocar filas existentes: así no se pisan favoritos ni cambios del usuario. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertSeedIgnoring(foods: List<NutritionFoodEntity>)

    @Query("UPDATE nutrition_food SET isFavorite = :favorite WHERE id = :id")
    public suspend fun setFavorite(
        id: String,
        favorite: Boolean,
    )

    /** Sólo borra alimentos personalizados; la semilla es permanente. */
    @Query("DELETE FROM nutrition_food WHERE id = :id AND isCustom = 1")
    public suspend fun deleteCustomById(id: String)

    @Query("DELETE FROM nutrition_food WHERE isCustom = 1")
    public suspend fun deleteAllCustom()

    @Query("UPDATE nutrition_food SET isFavorite = 0 WHERE isFavorite = 1")
    public suspend fun clearFavorites()
}
