package com.example.lifet.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta de marca de respaldo para Android < 12 o cuando el color dinámico está desactivado.
internal val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF00695C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF4A635F),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF3949AB),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF8FAF9),
        onBackground = Color(0xFF191C1B),
        surface = Color(0xFFF8FAF9),
        onSurface = Color(0xFF191C1B),
    )

internal val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF4DB6AC),
        onPrimary = Color(0xFF00201C),
        primaryContainer = Color(0xFF004D43),
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = Color(0xFFB1CCC6),
        onSecondary = Color(0xFF1C3531),
        tertiary = Color(0xFF9FA8DA),
        onTertiary = Color(0xFF1A237E),
        background = Color(0xFF101413),
        onBackground = Color(0xFFE0E3E1),
        surface = Color(0xFF101413),
        onSurface = Color(0xFFE0E3E1),
    )
