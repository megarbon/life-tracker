package com.example.lifet.core.domain.fitness

import com.example.lifet.core.data.fitness.FitnessRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.fitness.FitnessCalculator
import com.example.lifet.core.model.fitness.FitnessSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Serie diaria de minutos activos, pasos y peso de un [DateRange] para gráficas e Insights. Sólo incluye días
 * hasta hoy; los días sin actividad aparecen con cero (no se omiten) para que las correlaciones vean los huecos.
 */
public class ObserveFitnessSeries
    @Inject
    constructor(
        private val repository: FitnessRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(range: DateRange): Flow<FitnessSeries> =
            repository
                .observeData()
                .map { data -> FitnessCalculator(data, LocalDate.now(clock)).series(range) }
                .distinctUntilChanged()
    }
