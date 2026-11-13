package com.example.lifet.core.domain.quit

import com.example.lifet.core.data.quit.QuitRepository
import com.example.lifet.core.model.quit.QuitCalculator
import com.example.lifet.core.model.quit.QuitTodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Lo que la pantalla Hoy muestra de Dejar de fumar y beber: el tiempo limpio del hábito principal, lo ahorrado en
 * total, el próximo hito y si hoy hubo antojos. `hasData = false` indica que aún no hay ningún hábito.
 *
 * Se recalcula cuando cambian los datos; `cleanSince` permite a la tarjeta de Hoy dibujar el contador en vivo sin
 * pedir una emisión nueva cada segundo.
 */
public class ObserveQuitTodaySummary
    @Inject
    constructor(
        private val repository: QuitRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<QuitTodaySummary> =
            repository
                .observeData()
                .map { data -> QuitCalculator(data, clock.instant(), clock.zone).todaySummary() }
                .distinctUntilChanged()
    }
