package com.example.lifet.core.domain.finance

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.core.model.finance.pendingTransactions
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Materializa las ocurrencias de las reglas recurrentes hasta el horizonte (fin de mes o 30 días, lo que
 * llegue más tarde). Las futuras quedan como previstas y pasan a reales solas al llegar su fecha.
 * Es idempotente: los ids son estables y cada regla recuerda hasta dónde generó, así que borrar una
 * ocurrencia no la vuelve a crear.
 */
public class SyncRecurringTransactions
    @Inject
    constructor(
        private val repository: FinanceRepository,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(): AppResult<Unit> =
            when (val read = runCatchingApp(onError = { AppError.Storage }) { repository.observeData().first() }) {
                is AppResult.Failure -> read
                is AppResult.Success -> generate(read.value.rules.filter { it.active })
            }

        private suspend fun generate(rules: List<RecurringRule>): AppResult<Unit> {
            val today = LocalDate.now(clock)
            val horizon = maxOf(YearMonth.from(today).atEndOfMonth(), today.plusDays(FinanceCalculator.UPCOMING_DAYS))
            val pending =
                rules.flatMap { rule ->
                    rule.pendingTransactions(rule.startDate, horizon, clock.instant(), clock.zone)
                }
            return repository.applyGenerated(pending, rules.map { it.id }, horizon)
        }
    }
