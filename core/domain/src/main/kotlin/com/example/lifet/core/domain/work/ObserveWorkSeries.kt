package com.example.lifet.core.domain.work

import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.work.WorkCalculator
import com.example.lifet.core.model.work.WorkSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Serie diaria de horas trabajadas, objetivo, saldo y foco de un [DateRange] para gráficas e Insights. Sólo incluye
 * días hasta hoy; los días sin jornada aparecen con cero (no se omiten) para que las correlaciones vean los huecos.
 * Una jornada que cruza la medianoche cuenta entera en el día en que empezó.
 */
public class ObserveWorkSeries
    @Inject
    constructor(
        private val repository: WorkRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(range: DateRange): Flow<WorkSeries> =
            repository
                .observeData()
                .map { data -> WorkCalculator(data, clock.instant(), clock.zone).series(range) }
                .distinctUntilChanged()
    }
