package com.example.lifet.feature.mood

import androidx.compose.runtime.Immutable
import com.example.lifet.core.domain.mood.MoodDraft
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodTags
import java.time.Instant
import java.time.ZoneId

/**
 * Estado del formulario de check-in (alta y edición). Las operaciones son puras para poder probarlas sin UI.
 * [id], [instant] y [zone] se conservan al editar: cambiar el texto no mueve la entrada en el diario.
 */
@Immutable
internal data class MoodFormState(
    val id: Long = 0,
    val instant: Instant? = null,
    val zone: ZoneId? = null,
    val level: MoodLevel? = null,
    val emotions: List<String> = emptyList(),
    val energy: Int? = null,
    val stress: Int? = null,
    val factors: List<String> = emptyList(),
    val note: String = "",
    val gratitude: String = "",
    val emotionInput: String = "",
    val factorInput: String = "",
    val suggestedEmotions: List<String> = emptyList(),
    val suggestedFactors: List<String> = emptyList(),
    val saving: Boolean = false,
) {
    val isEditing: Boolean get() = id != 0L
    val canSave: Boolean get() = level != null && !saving

    /** Predefinidas, propias usadas antes y las ya elegidas (por si vienen de una edición). */
    val emotionOptions: List<String>
        get() = (MoodTags.DEFAULT_EMOTIONS + suggestedEmotions + emotions).distinct()

    val factorOptions: List<String>
        get() = (MoodTags.DEFAULT_FACTORS + suggestedFactors + factors).distinct()

    fun toggleEmotion(key: String): MoodFormState = copy(emotions = emotions.toggled(key))

    fun toggleFactor(key: String): MoodFormState = copy(factors = factors.toggled(key))

    /** Alterna el valor: tocar el ya elegido lo quita, así se puede dejar sin valor. */
    fun withEnergy(value: Int): MoodFormState = copy(energy = if (energy == value) null else value)

    fun withStress(value: Int): MoodFormState = copy(stress = if (stress == value) null else value)

    /** Convierte el texto escrito en una etiqueta propia seleccionada; ignora textos vacíos. */
    fun addEmotionFromInput(): MoodFormState {
        val tag = MoodTags.normalize(emotionInput) ?: return copy(emotionInput = "")
        return copy(emotions = (emotions + tag).distinct(), emotionInput = "")
    }

    fun addFactorFromInput(): MoodFormState {
        val tag = MoodTags.normalize(factorInput) ?: return copy(factorInput = "")
        return copy(factors = (factors + tag).distinct(), factorInput = "")
    }

    /** Lo que queda escrito sin confirmar también se guarda: perderlo en silencio sería una sorpresa. */
    fun toDraft(): MoodDraft? {
        val pending = addEmotionFromInput().addFactorFromInput()
        return pending.level?.let { chosen ->
            MoodDraft(
                level = chosen,
                id = pending.id,
                instant = pending.instant,
                zone = pending.zone,
                emotions = pending.emotions,
                energy = pending.energy,
                stress = pending.stress,
                factors = pending.factors,
                note = pending.note,
                gratitude = pending.gratitude,
            )
        }
    }

    /** `true` si el usuario ha escrito o elegido algo más que el nivel (para pedir confirmación al descartar). */
    val hasDetail: Boolean
        get() =
            emotions.isNotEmpty() || factors.isNotEmpty() || energy != null || stress != null ||
                note.isNotBlank() || gratitude.isNotBlank() || emotionInput.isNotBlank() || factorInput.isNotBlank()

    private fun List<String>.toggled(key: String): List<String> = if (key in this) this - key else this + key

    companion object {
        fun from(entry: MoodEntry): MoodFormState =
            MoodFormState(
                id = entry.id,
                instant = entry.instant,
                zone = entry.zone,
                level = entry.level,
                emotions = entry.emotions,
                energy = entry.energy,
                stress = entry.stress,
                factors = entry.factors,
                note = entry.note,
                gratitude = entry.gratitude,
            )
    }
}

internal fun MoodEntry.toDraft(): MoodDraft =
    MoodDraft(
        level = level,
        id = id,
        instant = instant,
        zone = zone,
        emotions = emotions,
        energy = energy,
        stress = stress,
        factors = factors,
        note = note,
        gratitude = gratitude,
    )
