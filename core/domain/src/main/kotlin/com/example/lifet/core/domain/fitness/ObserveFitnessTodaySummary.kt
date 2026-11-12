package com.example.lifet.core.domain.fitness

import com.example.lifet.core.data.fitness.FitnessRepository
import com.example.lifet.core.model.fitness.FitnessCalculator
import com.example.lifet.core.model.fitness.FitnessTodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Lo que la pantalla Hoy muestra de Deporte: minutos activos de la semana frente al objetivo de 150, si hoy hubo
 * entreno y el último peso. `hasData = false` indica que el área aún no se ha usado. "Hoy" se calcula con el
 * [Clock] inyectado en cada emisión.
 */
public class ObserveFitnessTodaySummary
    @Inject
    constructor(
        private val repository: FitnessRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<FitnessTodaySummary> =
            repository
                .observeData()
                .map { data -> FitnessCalculator(data, LocalDate.now(clock)).todaySummary() }
                .distinctUntilChanged()
    }
