package com.example.lifet.core.data.mood

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.mood.MoodDao
import com.example.lifet.core.database.mood.MoodEntryEntity
import com.example.lifet.core.model.mood.MoodEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Margen de la consulta por instante: cubre cualquier diferencia de zona entre la consulta y la entrada. */
private const val ZONE_MARGIN_DAYS = 1L
private const val TAG_HISTORY_ROWS = 200

internal class DefaultMoodRepository
    @Inject
    constructor(
        private val dao: MoodDao,
        private val clock: Clock,
        @param:IoDispatcher private val io: CoroutineDispatcher,
    ) : MoodRepository {
        override fun observeAll(): Flow<List<MoodEntry>> = dao.observeAll().map { it.toModels() }.flowOn(io)

        override fun observeBetween(
            from: LocalDate,
            toExclusive: LocalDate,
            zone: ZoneId,
        ): Flow<List<MoodEntry>> {
            val start = from.minusDays(ZONE_MARGIN_DAYS).atStartOfDay(zone).toInstant()
            val end = toExclusive.plusDays(ZONE_MARGIN_DAYS).atStartOfDay(zone).toInstant()
            return dao
                .observeBetween(start, end)
                .map { rows -> rows.toModels().filter { it.localDate >= from && it.localDate < toExclusive } }
                .flowOn(io)
        }

        override fun observeEntry(id: Long): Flow<MoodEntry?> = dao.observeById(id).map { it?.toModel() }.flowOn(io)

        override suspend fun save(entry: MoodEntry): AppResult<Long> =
            runCatchingApp(onError = { AppError.Storage }) {
                withContext(io) { dao.upsert(entry.toEntity(updatedAt = clock.instant())) }
            }

        override suspend fun delete(id: Long): AppResult<Unit> =
            runCatchingApp(onError = { AppError.Storage }) {
                withContext(io) { dao.deleteById(id) }
            }.let { result ->
                when (result) {
                    is AppResult.Failure -> result
                    is AppResult.Success -> if (result.value > 0) AppResult.Success(Unit) else notFound()
                }
            }

        override suspend fun saveAll(entries: List<MoodEntry>): AppResult<Unit> =
            runCatchingApp(onError = { AppError.Storage }) {
                val now = clock.instant()
                withContext(io) { dao.insertAll(entries.map { it.toEntity(updatedAt = now) }) }
            }

        override suspend fun clear(): AppResult<Unit> =
            runCatchingApp(onError = { AppError.Storage }) { withContext(io) { dao.deleteAll() } }

        override suspend fun tagHistory(): AppResult<MoodTagHistory> =
            runCatchingApp(onError = { AppError.Storage }) {
                withContext(io) {
                    MoodTagHistory(
                        emotions = dao.recentEmotions(TAG_HISTORY_ROWS).distinctTags(),
                        factors = dao.recentFactors(TAG_HISTORY_ROWS).distinctTags(),
                    )
                }
            }

        private fun notFound(): AppResult.Failure = AppResult.Failure(AppError.NotFound)

        private fun List<MoodEntryEntity>.toModels(): List<MoodEntry> = mapNotNull { it.toModel() }

        private fun List<String>.distinctTags(): List<String> = flatMap(::splitTags).distinct()
    }
