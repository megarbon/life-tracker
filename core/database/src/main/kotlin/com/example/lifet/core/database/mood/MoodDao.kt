package com.example.lifet.core.database.mood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Acceso a `mood_entry`. Los rangos son semiabiertos `[from, to)` sobre el instante UTC. */
@Dao
public interface MoodDao {
    @Query("SELECT * FROM mood_entry ORDER BY instant DESC")
    public fun observeAll(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entry WHERE instant >= :from AND instant < :to ORDER BY instant DESC")
    public fun observeBetween(
        from: Instant,
        to: Instant,
    ): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entry WHERE id = :id")
    public fun observeById(id: Long): Flow<MoodEntryEntity?>

    @Query("SELECT * FROM mood_entry WHERE id = :id")
    public suspend fun getById(id: Long): MoodEntryEntity?

    /** Etiquetas usadas recientemente, para sugerir las propias del usuario. */
    @Query("SELECT emotions FROM mood_entry ORDER BY instant DESC LIMIT :limit")
    public suspend fun recentEmotions(limit: Int): List<String>

    @Query("SELECT factors FROM mood_entry ORDER BY instant DESC LIMIT :limit")
    public suspend fun recentFactors(limit: Int): List<String>

    @Upsert
    public suspend fun upsert(entity: MoodEntryEntity): Long

    @Insert
    public suspend fun insertAll(entities: List<MoodEntryEntity>)

    @Query("DELETE FROM mood_entry WHERE id = :id")
    public suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM mood_entry")
    public suspend fun deleteAll()
}
