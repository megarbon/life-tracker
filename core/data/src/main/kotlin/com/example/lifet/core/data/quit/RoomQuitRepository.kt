package com.example.lifet.core.data.quit

import android.database.SQLException
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.quit.QuitDao
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("TooManyFunctions") // Implementa QuitRepository.
internal class RoomQuitRepository
    @Inject
    constructor(
        private val dao: QuitDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : QuitRepository {
        override fun observeData(): Flow<QuitDataSet> =
            combine(dao.observeHabits(), dao.observeCravings(), dao.observeConsumptions()) { habits, cravings, uses ->
                QuitDataSet(
                    habits = habits.map { it.toDomain() },
                    cravings = cravings.map { it.toDomain() },
                    consumptions = uses.map { it.toDomain() },
                )
            }.flowOn(ioDispatcher)

        override suspend fun saveHabit(habit: QuitHabit): AppResult<Unit> = write { dao.upsertHabit(habit.toEntity()) }

        override suspend fun deleteHabit(id: String): AppResult<Unit> = write { dao.deleteHabit(id) }

        override suspend fun restoreHabit(
            habit: QuitHabit,
            cravings: List<QuitCraving>,
            consumptions: List<QuitConsumption>,
        ): AppResult<Unit> =
            write {
                dao.restoreHabit(habit.toEntity(), cravings.map { it.toEntity() }, consumptions.map { it.toEntity() })
            }

        override suspend fun saveCraving(craving: QuitCraving): AppResult<Unit> =
            write { dao.upsertCraving(craving.toEntity()) }

        override suspend fun deleteCraving(id: String): AppResult<Unit> = write { dao.deleteCraving(id) }

        override suspend fun saveConsumption(consumption: QuitConsumption): AppResult<Unit> =
            write { dao.upsertConsumption(consumption.toEntity()) }

        override suspend fun deleteConsumption(id: String): AppResult<Unit> = write { dao.deleteConsumption(id) }

        override suspend fun replaceAll(data: QuitDataSet): AppResult<Unit> =
            write {
                dao.replaceAll(
                    habits = data.habits.map { it.toEntity() },
                    cravings = data.cravings.map { it.toEntity() },
                    consumptions = data.consumptions.map { it.toEntity() },
                )
            }

        private suspend fun write(block: suspend () -> Unit): AppResult<Unit> =
            withContext(ioDispatcher) {
                runCatchingApp(onError = { if (it is SQLException) AppError.Storage else AppError.Unknown }) { block() }
            }
    }
