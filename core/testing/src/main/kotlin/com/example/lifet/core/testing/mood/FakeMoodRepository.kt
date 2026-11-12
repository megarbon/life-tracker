package com.example.lifet.core.testing.mood

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.data.mood.MoodTagHistory
import com.example.lifet.core.model.mood.MoodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repositorio de ánimo en memoria. [failObserve] hace que los flujos fallen (para probar el estado de error)
 * y [failWrites] que guardar o borrar devuelva [AppError.Storage].
 */
public class FakeMoodRepository(
    initial: List<MoodEntry> = emptyList(),
) : MoodRepository {
    private val entries = MutableStateFlow(initial)
    private var nextId: Long = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    public var failObserve: Boolean = false
    public var failWrites: Boolean = false

    public val current: List<MoodEntry> get() = entries.value

    override fun observeAll(): Flow<List<MoodEntry>> =
        guarded { entries.map { it.sortedByDescending(MoodEntry::instant) } }

    override fun observeBetween(
        from: LocalDate,
        toExclusive: LocalDate,
        zone: ZoneId,
    ): Flow<List<MoodEntry>> =
        guarded {
            entries.map { list ->
                list
                    .filter { it.localDate >= from && it.localDate < toExclusive }
                    .sortedByDescending(MoodEntry::instant)
            }
        }

    override fun observeEntry(id: Long): Flow<MoodEntry?> =
        guarded { entries.map { list -> list.firstOrNull { it.id == id } } }

    override suspend fun save(entry: MoodEntry): AppResult<Long> {
        if (failWrites) return AppResult.Failure(AppError.Storage)
        val id = if (entry.id == 0L) nextId++ else entry.id
        entries.update { list -> list.filterNot { it.id == id } + entry.copy(id = id) }
        return AppResult.Success(id)
    }

    override suspend fun delete(id: Long): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Storage)
        val exists = entries.value.any { it.id == id }
        entries.update { list -> list.filterNot { it.id == id } }
        return if (exists) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun tagHistory(): AppResult<MoodTagHistory> {
        val sorted = entries.value.sortedByDescending(MoodEntry::instant)
        return AppResult.Success(
            MoodTagHistory(
                emotions = sorted.flatMap { it.emotions }.distinct(),
                factors = sorted.flatMap { it.factors }.distinct(),
            ),
        )
    }

    // Se evalúa al recolectar, así un test puede activar el fallo después de crear el ViewModel.
    private fun <T> guarded(block: () -> Flow<T>): Flow<T> =
        flow {
            check(!failObserve) { "Fallo simulado" }
            emitAll(block())
        }
}
