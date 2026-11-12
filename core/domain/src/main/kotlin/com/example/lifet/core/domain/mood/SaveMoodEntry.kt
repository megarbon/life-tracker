package com.example.lifet.core.domain.mood

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodTags
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Datos de un check-in tal y como salen de un formulario; `id = 0` crea y cualquier otro id edita. */
public data class MoodDraft(
    public val level: MoodLevel,
    public val id: Long = 0,
    public val instant: Instant? = null,
    public val zone: ZoneId? = null,
    public val emotions: List<String> = emptyList(),
    public val energy: Int? = null,
    public val stress: Int? = null,
    public val factors: List<String> = emptyList(),
    public val note: String = "",
    public val gratitude: String = "",
)

/**
 * Valida y guarda un check-in. Sin [MoodDraft.instant] usa el [Clock] inyectado (check-in "ahora").
 * Normaliza etiquetas (sin comas ni repetidos), recorta textos y rechaza escalas fuera de 1 a 5.
 */
public class SaveMoodEntry
    @Inject
    constructor(
        private val repository: MoodRepository,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(draft: MoodDraft): AppResult<Long> {
            val scale = MoodRules.SCALE_MIN..MoodRules.SCALE_MAX
            val invalid =
                (draft.energy != null && draft.energy !in scale) || (draft.stress != null && draft.stress !in scale)
            return if (invalid) {
                AppResult.Failure(AppError.Validation(REASON_SCALE))
            } else {
                repository.save(draft.toEntry())
            }
        }

        private fun MoodDraft.toEntry(): MoodEntry =
            MoodEntry(
                id = id,
                instant = instant ?: clock.instant(),
                zone = zone ?: clock.zone,
                level = level,
                emotions = emotions.cleanTags(),
                energy = energy,
                stress = stress,
                factors = factors.cleanTags(),
                note = note.trim().take(MoodRules.TEXT_MAX_LENGTH),
                gratitude = gratitude.trim().take(MoodRules.TEXT_MAX_LENGTH),
            )

        private fun List<String>.cleanTags(): List<String> =
            mapNotNull(MoodTags::normalize).distinct().take(MoodRules.MAX_TAGS)

        public companion object {
            public const val REASON_SCALE: String = "scale_out_of_range"
        }
    }
