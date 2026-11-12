package com.example.lifet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.lifet.core.model.Area

/**
 * Color de acento semántico por área. Sólo para iconos, progreso y gráficas; nunca para texto largo
 * ni como único portador de información.
 */
@Immutable
public data class AreaColors(
    val finance: Color,
    val fitness: Color,
    val nutrition: Color,
    val sleep: Color,
) {
    public fun forArea(area: Area): Color =
        when (area) {
            Area.FINANCE -> finance
            Area.FITNESS -> fitness
            Area.NUTRITION -> nutrition
            Area.SLEEP -> sleep
        }
}

// Finanzas = teal, deporte = naranja, comida = ámbar, sueño = índigo.
internal val LightAreaColors =
    AreaColors(
        finance = Color(0xFF00796B),
        fitness = Color(0xFFD84315),
        nutrition = Color(0xFFB26A00),
        sleep = Color(0xFF3949AB),
    )

internal val DarkAreaColors =
    AreaColors(
        finance = Color(0xFF4DB6AC),
        fitness = Color(0xFFFF8A65),
        nutrition = Color(0xFFFFCA28),
        sleep = Color(0xFF9FA8DA),
    )
