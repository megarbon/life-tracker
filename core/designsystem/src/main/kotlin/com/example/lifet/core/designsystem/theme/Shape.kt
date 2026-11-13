package com.example.lifet.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas del sistema: tarjetas y celdas de 16 dp, botones y campos de 12 dp, hojas de 28 dp.
 * Ver docs/DESIGN_GUIDE.md, sección 4.
 */
internal val LifeTrackerShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
