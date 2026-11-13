@file:Suppress("MagicNumber") // Valores ARGB literales: la paleta es dato, no regla.

package com.example.lifet.core.model.work

/**
 * Paleta cerrada de colores de proyecto. Se guarda el nombre del color (no el ARGB) para que ajustar la paleta no
 * deje datos huérfanos. El color nunca es el único portador de información: la UI siempre muestra el nombre.
 */
public enum class ProjectColor(
    public val argb: Long,
) {
    BLUE(0xFF1E88E5),
    TEAL(0xFF00897B),
    GREEN(0xFF43A047),
    AMBER(0xFFFFB300),
    ORANGE(0xFFF4511E),
    RED(0xFFE53935),
    PINK(0xFFD81B60),
    PURPLE(0xFF8E24AA),
    INDIGO(0xFF3949AB),
    GREY(0xFF757575),
}

/** Un proyecto al que se puede asignar una jornada o una sesión de foco. */
public data class WorkProject(
    public val id: String,
    public val name: String,
    public val color: ProjectColor = ProjectColor.BLUE,
)
