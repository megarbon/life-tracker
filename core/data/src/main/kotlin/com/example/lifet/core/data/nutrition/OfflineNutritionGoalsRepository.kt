package com.example.lifet.core.data.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.nutrition.NutritionGoalsDao
import com.example.lifet.core.model.nutrition.NutritionGoals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Los objetivos viven en Room (no en DataStore) para que el borrado de datos y los escenarios de demo los
 * restablezcan junto con el resto del área, en una sola transacción lógica.
 */
internal class OfflineNutritionGoalsRepository
    @Inject
    constructor(
        private val dao: NutritionGoalsDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : NutritionGoalsRepository {
        override fun observeGoals(): Flow<NutritionGoals> =
            dao
                .observe()
                .map { it?.toDomain() ?: NutritionGoals.DEFAULT }
                .distinctUntilChanged()
                .flowOn(ioDispatcher)

        override suspend fun setGoals(goals: NutritionGoals): AppResult<Unit> =
            if (!goals.isValid) {
                AppResult.Failure(AppError.Validation(GOALS_OUT_OF_RANGE))
            } else {
                withContext(ioDispatcher) {
                    runCatchingApp(onError = { AppError.Storage }) { dao.upsert(goals.toEntity()) }
                }
            }

        override suspend fun clearGoals(): AppResult<Unit> =
            withContext(ioDispatcher) { runCatchingApp(onError = { AppError.Storage }) { dao.deleteAll() } }

        companion object {
            const val GOALS_OUT_OF_RANGE = "goals_out_of_range"
        }
    }
