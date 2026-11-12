package com.example.lifet.core.domain.finance

import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.FinanceTodaySummary
import com.example.lifet.core.model.finance.primaryCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Lo que la pantalla Hoy muestra de Finanzas: gasto de hoy, disponible del mes y presupuesto restante,
 * en la divisa principal. `hasData = false` indica que el área aún no se ha usado.
 */
public class ObserveFinanceTodaySummary
    @Inject
    constructor(
        private val repository: FinanceRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<FinanceTodaySummary> =
            repository
                .observeData()
                .map { data -> FinanceCalculator(data, data.primaryCurrency(), LocalDate.now(clock)).todaySummary() }
                .distinctUntilChanged()
    }
