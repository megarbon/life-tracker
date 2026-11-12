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
    val nutrition: Color,
    val fitness: Color,
    val quit: Color,
    val work: Color,
    val mood: Color,
) {
    public fun forArea(area: Area): Color =
        when (area) {
            Area.FINANCE -> finance
            Area.NUTRITION -> nutrition
            Area.FITNESS -> fitness
            Area.QUIT -> quit
            Area.WORK -> work
            Area.MOOD -> mood
        }
}

// Finanzas = teal, comida = ámbar, deporte = naranja, dejar hábitos = verde, trabajo = azul, ánimo = rosa.
internal val LightAreaColors =
    AreaColors(
        finance = Color(0xFF00796B),
        nutrition = Color(0xFFB26A00),
        fitness = Color(0xFFD84315),
        quit = Color(0xFF2E7D32),
        work = Color(0xFF1565C0),
        mood = Color(0xFFAD1457),
    )

internal val DarkAreaColors =
    AreaColors(
        finance = Color(0xFF4DB6AC),
        nutrition = Color(0xFFFFCA28),
        fitness = Color(0xFFFF8A65),
        quit = Color(0xFF81C784),
        work = Color(0xFF64B5F6),
        mood = Color(0xFFF48FB1),
    )
