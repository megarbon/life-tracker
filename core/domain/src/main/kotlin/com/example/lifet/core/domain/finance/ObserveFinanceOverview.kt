package com.example.lifet.core.domain.finance

import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.LedgerFilter
import com.example.lifet.core.model.finance.primaryCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Datos de la pantalla de Finanzas para [month], recalculados con cada cambio en la base de datos. */
public class ObserveFinanceOverview
    @Inject
    constructor(
        private val repository: FinanceRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(
            month: YearMonth,
            filter: LedgerFilter = LedgerFilter(),
        ): Flow<FinanceOverview> =
            repository.observeData().map { data ->
                FinanceCalculator(data, data.primaryCurrency(), LocalDate.now(clock)).overview(month, filter)
            }
    }
