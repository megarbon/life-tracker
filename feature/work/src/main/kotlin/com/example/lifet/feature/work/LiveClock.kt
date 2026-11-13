package com.example.lifet.feature.work

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val MILLIS_PER_SECOND = 1_000L

/**
 * Reloj de la pantalla. Lo provee [WorkScreen] con el `Clock` inyectado en el ViewModel; el valor por defecto es un
 * reloj fijo para que un composable suelto (o una preview) nunca dependa de la hora real.
 */
internal val LocalWorkClock = staticCompositionLocalOf<Clock> { Clock.fixed(Instant.EPOCH, ZoneOffset.UTC) }

/**
 * "Ahora" que avanza cada segundo mientras [enabled] y la pantalla esté visible (se detiene en segundo plano).
 *
 * Quien lee el valor es el único que se recompone: el cronómetro lo lee en su propio composable hoja, de modo que un
 * segundo que pasa no recompone la pantalla entera. Con [enabled] a `false` no hay ningún temporizador en marcha.
 */
@Composable
internal fun rememberLiveNow(enabled: Boolean): State<Instant> {
    val clock = LocalWorkClock.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(initialValue = clock.instant(), clock, enabled, lifecycle) {
        value = clock.instant()
        if (enabled) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    value = clock.instant()
                    // Se alinea con el cambio de segundo para que el cronómetro no se salte ni repita cifras.
                    delay(MILLIS_PER_SECOND - value.toEpochMilli().mod(MILLIS_PER_SECOND))
                }
            }
        }
    }
}
