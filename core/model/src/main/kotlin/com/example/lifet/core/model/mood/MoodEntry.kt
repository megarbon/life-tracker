package com.example.lifet.core.model.mood

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Un check-in de ánimo. Puede haber varios por día.
 *
 * Se guarda el [instant] (UTC) junto con la [zone] del usuario en ese momento, de modo que el día lógico
 * ([localDate]) no cambia si el usuario viaja o cambia la zona del móvil.
 *
 * [emotions] y [factors] son claves estables: las de [MoodTags] son las predefinidas (se traducen en la UI)
 * y cualquier otra es una etiqueta propia del usuario que se muestra tal cual.
 * [energy] y [stress] van de 1 a 5 y son opcionales (el check-in rápido sólo lleva [level]).
 */
public data class MoodEntry(
    public val id: Long = 0,
    public val instant: Instant,
    public val zone: ZoneId,
    public val level: MoodLevel,
    public val emotions: List<String> = emptyList(),
    public val energy: Int? = null,
    public val stress: Int? = null,
    public val factors: List<String> = emptyList(),
    public val note: String = "",
    public val gratitude: String = "",
) {
    public val localDate: LocalDate get() = instant.atZone(zone).toLocalDate()

    /** `true` si el check-in lleva algo más que el nivel. */
    public val hasDetail: Boolean
        get() =
            emotions.isNotEmpty() || energy != null || stress != null || factors.isNotEmpty() ||
                note.isNotBlank() || gratitude.isNotBlank()
}
