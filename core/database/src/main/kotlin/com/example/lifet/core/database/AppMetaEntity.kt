package com.example.lifet.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Pares clave/valor internos (versión de los datos, onboarding completado…). No son preferencias de usuario. */
@Entity(tableName = "app_meta")
internal data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
internal interface AppMetaDao {
    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun upsert(entity: AppMetaEntity)
}
