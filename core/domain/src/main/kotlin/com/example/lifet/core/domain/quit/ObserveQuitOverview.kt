package com.example.lifet.core.domain.quit

import com.example.lifet.core.data.quit.QuitRepository
import com.example.lifet.core.model.quit.QuitCalculator
import com.example.lifet.core.model.quit.QuitOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Datos de la pantalla, recalculados con cada cambio en la base de datos. "Ahora" y la zona salen del [Clock]
 * inyectado en cada emisión. El contador en vivo no vuelve a pedir esto cada segundo: la pantalla lo reconstruye
 * con el instante de inicio de la racha, así que la base sólo se relee cuando cambia algo de verdad.
 */
public class ObserveQuitOverview
    @Inject
    constructor(
        private val repository: QuitRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<QuitOverview> =
            repository.observeData().map { data -> QuitCalculator(data, clock.instant(), clock.zone).overview() }
    }
