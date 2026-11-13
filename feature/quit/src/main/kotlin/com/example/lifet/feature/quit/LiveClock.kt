package com.example.lifet.feature.quit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Instant

/**
 * "Ahora" que avanza mientras la pantalla está visible: se actualiza justo cuando cambia lo que se ve (cada segundo el
 * primer día y cada minuto después) y se detiene al pasar a segundo plano. Devuelve un [State] para que sólo
 * recomponga quien lo lee, no la pantalla entera.
 *
 * [nextDelayMillis] dice cuánto falta para el próximo cambio visible a partir del instante actual.
 */
@Composable
internal fun rememberLiveNow(
    clock: Clock,
    nextDelayMillis: (Instant) -> Long,
): State<Instant> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now = remember(clock) { mutableStateOf(clock.instant()) }
    var current by now
    LaunchedEffect(clock, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                current = clock.instant()
                delay(nextDelayMillis(current))
            }
        }
    }
    return now
}
