package com.example.lifet.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

internal val LocalAreaColors = staticCompositionLocalOf { LightAreaColors }
internal val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Acceso a los tokens propios del design system: `LifeTrackerTheme.spacing`, `LifeTrackerTheme.areaColors`. */
public object LifeTrackerTheme {
    public val areaColors: AreaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAreaColors.current

    public val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}

/**
 * Tema de la app: paleta de marca estilo lista agrupada de iOS. El color dinámico (Android 12+) es opcional y viene
 * apagado para mantener la identidad visual (ver docs/DESIGN_GUIDE.md). Los colores de área nunca son dinámicos.
 */
@Composable
public fun LifeTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColorScheme
            }

            else -> {
                LightColorScheme
            }
        }

    CompositionLocalProvider(
        LocalAreaColors provides if (darkTheme) DarkAreaColors else LightAreaColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeTrackerTypography,
            shapes = LifeTrackerShapes,
            content = content,
        )
    }
}
