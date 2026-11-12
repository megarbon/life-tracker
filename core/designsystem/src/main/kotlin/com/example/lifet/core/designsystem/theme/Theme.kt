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
 * Tema de la app: Material 3 con color dinámico (Android 12+) y paleta de marca como respaldo.
 * Los colores de área no son dinámicos a propósito: mantienen su significado semántico.
 */
@Composable
public fun LifeTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
            content = content,
        )
    }
}
