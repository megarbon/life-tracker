package com.example.lifet.core.data.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.nutrition.NutritionGoals
import kotlinx.coroutines.flow.Flow

/** Objetivos diarios de energía, macros y agua. Si el usuario aún no los ha fijado, emite [NutritionGoals.DEFAULT]. */
public interface NutritionGoalsRepository {
    public fun observeGoals(): Flow<NutritionGoals>

    /** Falla con `AppError.Validation("goals_out_of_range")` si algún valor no es razonable. */
    public suspend fun setGoals(goals: NutritionGoals): AppResult<Unit>

    /** Vuelve a los objetivos por defecto. */
    public suspend fun clearGoals(): AppResult<Unit>
}
