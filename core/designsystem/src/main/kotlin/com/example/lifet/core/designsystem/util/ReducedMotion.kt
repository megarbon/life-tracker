package com.example.lifet.core.designsystem.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `true` cuando el usuario ha desactivado las animaciones del sistema ("Eliminar animaciones").
 * Los componentes con movimiento (shimmer, anillos) deben mostrar el estado final sin animar.
 */
@Composable
public fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
