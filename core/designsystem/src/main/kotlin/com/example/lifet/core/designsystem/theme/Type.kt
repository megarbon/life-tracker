package com.example.lifet.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/** Cifras tabulares para que los números no "bailen" al cambiar y alineen en columnas. */
private const val TABULAR_NUMBERS = "tnum"

internal val LifeTrackerTypography: Typography =
    Typography().let { base ->
        base.copy(
            displaySmall = base.displaySmall.tabular(),
            headlineMedium = base.headlineMedium.tabular(),
            titleLarge = base.titleLarge.tabular(),
        )
    }

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_NUMBERS)
