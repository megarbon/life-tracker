package com.example.lifet.core.domain.work

import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.work.WorkCalculator
import com.example.lifet.core.model.work.WorkTodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Lo que la pantalla Hoy muestra de Trabajo: si hay una jornada en curso (y su entrada), las horas de hoy, el saldo
 * de la semana frente al objetivo y si hay jornadas olvidadas por cerrar. `hasData = false` indica que el área aún
 * no se ha usado. "Ahora" y "hoy" salen del [Clock] inyectado en cada emisión.
 *
 * Con una jornada en marcha el resumen no vuelve a emitir cada segundo: lleva su `asOf` y `todaySecondsAt(now)` /
 * `shiftSecondsAt(now)` suman lo transcurrido, de modo que un cronómetro vivo sólo necesita un `Instant`.
 */
public class ObserveWorkTodaySummary
    @Inject
    constructor(
        private val repository: WorkRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<WorkTodaySummary> =
            repository
                .observeData()
                .map { data -> WorkCalculator(data, clock.instant(), clock.zone).todaySummary() }
                .distinctUntilChanged()
    }
