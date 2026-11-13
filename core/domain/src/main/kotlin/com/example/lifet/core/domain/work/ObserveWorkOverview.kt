package com.example.lifet.core.domain.work

import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.work.WorkCalculator
import com.example.lifet.core.model.work.WorkOverview
import com.example.lifet.core.model.work.WorkWeekReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/** Datos de las pestañas Hoy, Foco y Proyectos, recalculados con cada cambio en la base de datos. */
public class ObserveWorkOverview
    @Inject
    constructor(
        private val repository: WorkRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<WorkOverview> =
            repository.observeData().map { data -> WorkCalculator(data, clock.instant(), clock.zone).overview() }
    }

/** Informe de la semana (lunes a domingo) que contiene [DateRange.start]: horas por día, saldo y reparto. */
public class ObserveWorkWeek
    @Inject
    constructor(
        private val repository: WorkRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(week: DateRange): Flow<WorkWeekReport> =
            repository.observeData().map { data -> WorkCalculator(data, clock.instant(), clock.zone).weekReport(week) }
    }
