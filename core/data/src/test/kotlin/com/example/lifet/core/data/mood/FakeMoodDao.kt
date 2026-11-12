package com.example.lifet.core.data.mood

import com.example.lifet.core.database.mood.MoodDao
import com.example.lifet.core.database.mood.MoodEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/** DAO en memoria para probar el repositorio sin Room. [failWrites] simula un fallo de almacenamiento. */
class FakeMoodDao(
    private val failWrites: Boolean = false,
) : MoodDao {
    private val rows = MutableStateFlow<List<MoodEntryEntity>>(emptyList())
    private var nextId = 1L

    private fun sorted() = rows.map { list -> list.sortedByDescending { it.instant } }

    override fun observeAll(): Flow<List<MoodEntryEntity>> = sorted()

    override fun observeBetween(
        from: Instant,
        to: Instant,
    ): Flow<List<MoodEntryEntity>> = sorted().map { list -> list.filter { it.instant >= from && it.instant < to } }

    override fun observeById(id: Long): Flow<MoodEntryEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): MoodEntryEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun recentEmotions(limit: Int): List<String> =
        rows.value.sortedByDescending { it.instant }.take(limit).map { it.emotions }

    override suspend fun recentFactors(limit: Int): List<String> =
        rows.value.sortedByDescending { it.instant }.take(limit).map { it.factors }

    override suspend fun upsert(entity: MoodEntryEntity): Long {
        check(!failWrites) { "Fallo simulado" }
        val id = if (entity.id == 0L) nextId++ else entity.id
        rows.update { list -> list.filterNot { it.id == id } + entity.copy(id = id) }
        return id
    }

    override suspend fun insertAll(entities: List<MoodEntryEntity>) {
        entities.forEach { upsert(it) }
    }

    override suspend fun deleteById(id: Long): Int {
        val before = rows.value.size
        rows.update { list -> list.filterNot { it.id == id } }
        return before - rows.value.size
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}
