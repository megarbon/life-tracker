package com.example.lifet.core.data.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.nutrition.FoodItem
import kotlinx.coroutines.flow.Flow

/**
 * Base de alimentos local: la semilla incluida en la app más los alimentos personalizados del usuario.
 * La semilla se carga sola la primera vez que se observa cualquier flujo de este repositorio.
 */
public interface FoodRepository {
    /** Alimentos cuyo nombre contiene [query] (sin distinguir mayúsculas), con los favoritos primero. */
    public fun searchFoods(query: String): Flow<List<FoodItem>>

    public fun observeFood(id: String): Flow<FoodItem?>

    /** Últimos alimentos distintos registrados en el diario, del más reciente al más antiguo. */
    public fun observeRecentFoods(limit: Int): Flow<List<FoodItem>>

    public fun observeFavoriteFoods(): Flow<List<FoodItem>>

    public suspend fun setFavorite(
        id: String,
        favorite: Boolean,
    ): AppResult<Unit>

    /** Crea o actualiza un alimento personalizado (siempre con `isCustom = true`). */
    public suspend fun saveCustomFood(food: FoodItem): AppResult<Unit>

    /** Borra un alimento personalizado; los de la semilla no se pueden borrar y se ignoran. */
    public suspend fun deleteCustomFood(id: String): AppResult<Unit>
}
