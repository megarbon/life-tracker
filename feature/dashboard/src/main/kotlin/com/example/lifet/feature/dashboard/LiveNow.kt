package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Instant

private const val MINUTE_MS = 60_000L

/**
 * "Ahora" que avanza cada minuto mientras Hoy está visible, para que las cifras de una jornada abierta no se queden
 * congeladas. Devuelve un [State] para que solo recomponga quien lo lee y se detiene al pasar a segundo plano.
 *
 * Nota: existen copias parecidas en `feature/quit` y `feature/work` con su propio ritmo; unificarlas en `core:ui`
 * está anotado en el backlog.
 */
@Composable
internal fun rememberMinuteNow(clock: Clock): State<Instant> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now = remember(clock) { mutableStateOf(clock.instant()) }
    LaunchedEffect(clock, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now.value = clock.instant()
                // Alinea el siguiente tick con el cambio de minuto real para que la cifra salte a tiempo.
                delay(MINUTE_MS - (clock.millis() % MINUTE_MS))
            }
        }
    }
    return now
}
