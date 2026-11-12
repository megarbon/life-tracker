package com.example.lifet.core.database.mood

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId

/**
 * Check-in de ánimo. Emociones y factores se guardan como claves separadas por comas: son listas cortas que
 * siempre se leen con su entrada, y las claves se normalizan sin comas antes de llegar aquí.
 */
@Entity(
    tableName = "mood_entry",
    indices = [Index(value = ["instant"])],
)
public data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instant: Instant,
    val zone: ZoneId,
    val level: Int,
    val emotions: String,
    val energy: Int?,
    val stress: Int?,
    val factors: String,
    val note: String,
    val gratitude: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
