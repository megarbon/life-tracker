package com.example.lifet.core.domain.finance

import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.FinanceSeries
import com.example.lifet.core.model.finance.primaryCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Serie diaria de ingresos, gasto neto y saldo confirmados para Insights. Sólo incluye días hasta hoy;
 * los días sin movimientos aparecen con cero (no se omiten) para que las correlaciones vean los huecos.
 */
public class ObserveFinanceSeries
    @Inject
    constructor(
        private val repository: FinanceRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(range: DateRange): Flow<FinanceSeries> =
            repository
                .observeData()
                .map { data -> FinanceCalculator(data, data.primaryCurrency(), LocalDate.now(clock)).series(range) }
                .distinctUntilChanged()
    }
