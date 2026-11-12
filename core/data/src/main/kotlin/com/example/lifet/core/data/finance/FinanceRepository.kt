package com.example.lifet.core.data.finance

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.RecurringRule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Fuente única de verdad de finanzas (Room). Las lecturas son reactivas; las escrituras devuelven
 * [AppResult] y nunca lanzan, salvo cancelación.
 */
@Suppress("TooManyFunctions") // Un repositorio por área (CRUD de seis entidades relacionadas).
public interface FinanceRepository {
    /** Todo el contenido de finanzas; vuelve a emitir con cada cambio. */
    public fun observeData(): Flow<FinanceDataSet>

    public suspend fun saveTransaction(transaction: FinanceTransaction): AppResult<Unit>

    public suspend fun deleteTransaction(id: String): AppResult<Unit>

    public suspend fun saveTransfer(transfer: FinanceTransfer): AppResult<Unit>

    public suspend fun deleteTransfer(id: String): AppResult<Unit>

    public suspend fun saveAccount(account: Account): AppResult<Unit>

    public suspend fun saveCategory(category: FinanceCategory): AppResult<Unit>

    /** Fija el límite mensual de [categoryId]; `null` lo elimina. */
    public suspend fun setBudget(
        categoryId: String,
        limit: Money?,
    ): AppResult<Unit>

    /** Guarda la regla y descarta sus ocurrencias posteriores a [today] para que se regeneren con los datos nuevos. */
    public suspend fun saveRule(
        rule: RecurringRule,
        today: LocalDate,
    ): AppResult<Unit>

    /** Borra la regla y sus ocurrencias futuras; las ya pasadas se quedan como movimientos normales. */
    public suspend fun deleteRule(
        id: String,
        today: LocalDate,
    ): AppResult<Unit>

    /** Crea las categorías de partida y la cuenta principal si aún no existen. */
    public suspend fun ensureDefaults(): AppResult<Unit>

    /** Inserta los movimientos de [transactions] que falten y marca cada regla como generada hasta [through]. */
    public suspend fun applyGenerated(
        transactions: List<FinanceTransaction>,
        ruleIds: List<String>,
        through: LocalDate,
    ): AppResult<Unit>

    /** Sustituye todos los datos de finanzas (demo, restaurar copia). */
    public suspend fun replaceAll(data: FinanceDataSet): AppResult<Unit>
}
