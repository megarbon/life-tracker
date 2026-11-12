package com.example.lifet.core.testing.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.model.nutrition.NutritionGoals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Objetivos en memoria con la misma validación que la implementación real. */
public class FakeNutritionGoalsRepository(
    initial: NutritionGoals = NutritionGoals.DEFAULT,
) : NutritionGoalsRepository {
    public val goals: MutableStateFlow<NutritionGoals> = MutableStateFlow(initial)
    public var failWrites: Boolean = false

    override fun observeGoals(): Flow<NutritionGoals> = goals

    override suspend fun setGoals(goals: NutritionGoals): AppResult<Unit> =
        when {
            failWrites -> AppResult.Failure(AppError.Storage)
            !goals.isValid -> AppResult.Failure(AppError.Validation("goals_out_of_range"))
            else -> AppResult.Success(Unit).also { this.goals.value = goals }
        }

    override suspend fun clearGoals(): AppResult<Unit> =
        AppResult.Success(Unit).also {
            goals.value =
                NutritionGoals.DEFAULT
        }
}
