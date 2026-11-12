package com.example.lifet.core.domain.fitness

import com.example.lifet.core.data.fitness.FitnessRepository
import com.example.lifet.core.model.fitness.FitnessCalculator
import com.example.lifet.core.model.fitness.FitnessOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Datos de la pantalla de Deporte, recalculados con cada cambio en la base de datos. */
public class ObserveFitnessOverview
    @Inject
    constructor(
        private val repository: FitnessRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<FitnessOverview> =
            repository.observeData().map { data -> FitnessCalculator(data, LocalDate.now(clock)).overview() }
    }
