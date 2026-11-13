package com.example.lifet.core.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

/**
 * La fecha de "hoy" como flujo: emite la actual al suscribirse y vuelve a emitir al llegar cada medianoche.
 *
 * Las pantallas que dependen del día (Hoy) deben recolectar esto con `flatMapLatest` en lugar de fijar la fecha
 * al crearse, o se quedarían mostrando el día anterior si la app sigue viva pasada la medianoche.
 * Límite conocido: la zona del [Clock] se fija al crearlo, así que un cambio de zona horaria con la app abierta
 * no se refleja hasta reiniciarla.
 */
public class ObserveToday
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<LocalDate> =
            flow {
                while (true) {
                    val today = LocalDate.now(clock)
                    emit(today)
                    delay(untilNextDay(today).toMillis().coerceAtLeast(MIN_WAIT_MS))
                }
            }.distinctUntilChanged()

        private fun untilNextDay(today: LocalDate): Duration {
            val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
            return Duration.between(clock.instant(), nextMidnight)
        }

        private companion object {
            // Evita un bucle ocupado si el reloj devuelve un instante ya pasado la medianoche por redondeo.
            const val MIN_WAIT_MS = 1_000L
        }
    }
