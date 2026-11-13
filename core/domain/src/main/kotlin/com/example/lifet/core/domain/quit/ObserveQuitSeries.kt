package com.example.lifet.core.domain.quit

import com.example.lifet.core.data.quit.QuitRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.quit.QuitCalculator
import com.example.lifet.core.model.quit.QuitSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Serie diaria de antojos, consumos y días limpios de un [DateRange], para gráficas e Insights. Sólo incluye días
 * hasta hoy; los días sin datos aparecen con cero (no se omiten) para que las correlaciones vean los huecos.
 */
public class ObserveQuitSeries
    @Inject
    constructor(
        private val repository: QuitRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(range: DateRange): Flow<QuitSeries> =
            repository
                .observeData()
                .map { data -> QuitCalculator(data, clock.instant(), clock.zone).series(range) }
                .distinctUntilChanged()
    }
