package com.example.lifet.core.model.mood

/**
 * Catálogo de emociones y factores predefinidos (por clave) y normalización de etiquetas propias.
 * Las claves se traducen en la capa de UI; aquí no hay texto de usuario.
 */
public object MoodTags {
    public const val MAX_LENGTH: Int = 30

    public val DEFAULT_EMOTIONS: List<String> =
        listOf(
            "joy",
            "calm",
            "gratitude",
            "motivation",
            "love",
            "pride",
            "tired",
            "anxiety",
            "sadness",
            "anger",
            "loneliness",
            "boredom",
        )

    public val DEFAULT_FACTORS: List<String> =
        listOf("sleep", "sport", "work", "social", "family", "health", "food", "money", "nature", "hobbies")

    /** Emociones del lado agradable; el resto se usa al generar datos de demo y para sugerencias. */
    public val POSITIVE_EMOTIONS: Set<String> = setOf("joy", "calm", "gratitude", "motivation", "love", "pride")

    /**
     * Limpia una etiqueta escrita por el usuario: sin espacios sobrantes, sin comas (se usan como separador al
     * persistir) y con longitud acotada. Devuelve `null` si queda vacía.
     */
    public fun normalize(raw: String): String? =
        raw
            .replace(',', ' ')
            .trim()
            .replace(WHITESPACE, " ")
            .take(MAX_LENGTH)
            .trim()
            .ifEmpty { null }

    private val WHITESPACE = Regex("\\s+")
}
