package com.example.lifet.core.data.mood

import com.example.lifet.core.database.mood.MoodEntryEntity
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import java.time.Instant

private const val TAG_SEPARATOR = ","

internal fun splitTags(raw: String): List<String> =
    raw.split(TAG_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

internal fun joinTags(tags: List<String>): String = tags.joinToString(TAG_SEPARATOR)

/** Devuelve `null` si el nivel guardado es inválido: una fila corrupta se ignora en vez de romper la pantalla. */
internal fun MoodEntryEntity.toModel(): MoodEntry? {
    val mood = MoodLevel.fromValue(level) ?: return null
    return MoodEntry(
        id = id,
        instant = instant,
        zone = zone,
        level = mood,
        emotions = splitTags(emotions),
        energy = energy,
        stress = stress,
        factors = splitTags(factors),
        note = note,
        gratitude = gratitude,
    )
}

internal fun MoodEntry.toEntity(updatedAt: Instant): MoodEntryEntity =
    MoodEntryEntity(
        id = id,
        instant = instant,
        zone = zone,
        level = level.value,
        emotions = joinTags(emotions),
        energy = energy,
        stress = stress,
        factors = joinTags(factors),
        note = note,
        gratitude = gratitude,
        updatedAt = updatedAt,
    )
