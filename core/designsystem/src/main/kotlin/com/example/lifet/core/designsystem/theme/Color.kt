package com.example.lifet.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta de marca estilo "lista agrupada" de iOS: un fondo y, encima, celdas (tarjetas) con un tono propio.
// El acento es el teal de la marca; el rojo solo se usa para errores y acciones destructivas.
// Ver docs/DESIGN_GUIDE.md, sección 2.

private val GroupedBackgroundLight = Color(0xFFF2F2F7)
private val CellLight = Color(0xFFFFFFFF)
private val CellRaisedLight = Color(0xFFEFEFF4)
private val CellHighestLight = Color(0xFFE5E5EA)
private val LabelLight = Color(0xFF000000)
private val LabelSecondaryLight = Color(0xFF6C6C70)

private val GroupedBackgroundDark = Color(0xFF000000)
private val CellDark = Color(0xFF1C1C1E)
private val CellRaisedDark = Color(0xFF2C2C2E)
private val CellHighestDark = Color(0xFF3A3A3C)
private val LabelDark = Color(0xFFFFFFFF)
private val LabelSecondaryDark = Color(0xFFAEAEB2)

internal val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF00695C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD7EFEA),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF4A635F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE3EDEB),
        onSecondaryContainer = Color(0xFF0E1F1C),
        tertiary = Color(0xFF3949AB),
        onTertiary = Color(0xFFFFFFFF),
        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF),
        background = GroupedBackgroundLight,
        onBackground = LabelLight,
        surface = GroupedBackgroundLight,
        onSurface = LabelLight,
        onSurfaceVariant = LabelSecondaryLight,
        surfaceVariant = CellRaisedLight,
        surfaceContainerLowest = CellLight,
        surfaceContainerLow = CellLight,
        surfaceContainer = CellLight,
        surfaceContainerHigh = CellRaisedLight,
        surfaceContainerHighest = CellHighestLight,
        outline = Color(0xFFC6C6C8),
        outlineVariant = Color(0xFFE0E0E4),
    )

internal val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF4DB6AC),
        onPrimary = Color(0xFF00201C),
        primaryContainer = Color(0xFF0B3B35),
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = Color(0xFFB1CCC6),
        onSecondary = Color(0xFF1C3531),
        secondaryContainer = Color(0xFF2A3B38),
        onSecondaryContainer = Color(0xFFD2E8E3),
        tertiary = Color(0xFF9FA8DA),
        onTertiary = Color(0xFF1A237E),
        error = Color(0xFFEF5350),
        onError = Color(0xFF3B0000),
        background = GroupedBackgroundDark,
        onBackground = LabelDark,
        surface = GroupedBackgroundDark,
        onSurface = LabelDark,
        onSurfaceVariant = LabelSecondaryDark,
        surfaceVariant = CellRaisedDark,
        surfaceContainerLowest = GroupedBackgroundDark,
        surfaceContainerLow = CellDark,
        surfaceContainer = CellDark,
        surfaceContainerHigh = CellRaisedDark,
        surfaceContainerHighest = CellHighestDark,
        outline = Color(0xFF48484A),
        outlineVariant = Color(0xFF38383A),
    )
