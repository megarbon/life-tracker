package com.example.lifet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaciado en múltiplos de 4dp. */
@Immutable
public data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

/** Tamaño mínimo de un objetivo táctil accesible. */
public val MinTouchTarget: Dp = 48.dp
