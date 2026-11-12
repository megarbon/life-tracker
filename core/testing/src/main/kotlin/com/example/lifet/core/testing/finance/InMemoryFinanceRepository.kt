package com.example.lifet.core.testing.finance

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.Budget
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.core.model.finance.defaultFinanceCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Repositorio en memoria con las mismas reglas que el de Room. Sirve para tests de dominio, ViewModels y
 * generadores de demo sin necesidad de base de datos. Con [failWrites] las escrituras devuelven un error de
 * almacenamiento, para probar los estados de error.
 */
@Suppress("TooManyFunctions") // Implementa FinanceRepository.
public class InMemoryFinanceRepository(
    initial: FinanceDataSet = FinanceDataSet(),
) : FinanceRepository {
    private val state = MutableStateFlow(initial)

    public var failWrites: Boolean = false

    public val current: FinanceDataSet get() = state.value

    override fun observeData(): Flow<FinanceDataSet> = state

    override suspend fun saveTransaction(transaction: FinanceTransaction): AppResult<Unit> =
        edit { data -> data.copy(transactions = data.transactions.replacing(transaction) { it.id == transaction.id }) }

    override suspend fun deleteTransaction(id: String): AppResult<Unit> =
        edit { data -> data.copy(transactions = data.transactions.filterNot { it.id == id }) }

    override suspend fun saveTransfer(transfer: FinanceTransfer): AppResult<Unit> =
        edit { data -> data.copy(transfers = data.transfers.replacing(transfer) { it.id == transfer.id }) }

    override suspend fun deleteTransfer(id: String): AppResult<Unit> =
        edit { data -> data.copy(transfers = data.transfers.filterNot { it.id == id }) }

    override suspend fun saveAccount(account: Account): AppResult<Unit> =
        edit { data -> data.copy(accounts = data.accounts.replacing(account) { it.id == account.id }) }

    override suspend fun saveCategory(category: FinanceCategory): AppResult<Unit> =
        edit { data -> data.copy(categories = data.categories.replacing(category) { it.id == category.id }) }

    override suspend fun setBudget(
        categoryId: String,
        limit: Money?,
    ): AppResult<Unit> =
        edit { data ->
            val others = data.budgets.filterNot { it.categoryId == categoryId }
            data.copy(budgets = if (limit == null) others else others + Budget(categoryId, limit))
        }

    override suspend fun saveRule(
        rule: RecurringRule,
        today: LocalDate,
    ): AppResult<Unit> =
        edit { data ->
            val rewound = rule.generatedThrough?.let { if (it > today) today else it }
            data.copy(
                transactions = data.transactions.filterNot { it.recurringRuleId == rule.id && it.date > today },
                rules = data.rules.replacing(rule.copy(generatedThrough = rewound)) { it.id == rule.id },
            )
        }

    override suspend fun deleteRule(
        id: String,
        today: LocalDate,
    ): AppResult<Unit> =
        edit { data ->
            data.copy(
                transactions = data.transactions.filterNot { it.recurringRuleId == id && it.date > today },
                rules = data.rules.filterNot { it.id == id },
            )
        }

    override suspend fun ensureDefaults(): AppResult<Unit> =
        edit { data ->
            data.copy(
                categories = data.categories.ifEmpty { defaultFinanceCategories() },
                accounts =
                    data.accounts.ifEmpty {
                        listOf(Account(Account.DEFAULT_ID, "", AccountType.BANK, Money.zero(CurrencyCode.EUR)))
                    },
            )
        }

    override suspend fun applyGenerated(
        transactions: List<FinanceTransaction>,
        ruleIds: List<String>,
        through: LocalDate,
    ): AppResult<Unit> =
        edit { data ->
            val existing = data.transactions.mapTo(mutableSetOf()) { it.id }
            data.copy(
                transactions = data.transactions + transactions.filter { existing.add(it.id) },
                rules = data.rules.map { if (it.id in ruleIds) it.copy(generatedThrough = through) else it },
            )
        }

    override suspend fun replaceAll(data: FinanceDataSet): AppResult<Unit> = edit { data }

    private fun edit(change: (FinanceDataSet) -> FinanceDataSet): AppResult<Unit> =
        if (failWrites) {
            AppResult.Failure(AppError.Storage)
        } else {
            state.update(change)
            AppResult.Success(Unit)
        }

    private inline fun <T> List<T>.replacing(
        item: T,
        matches: (T) -> Boolean,
    ): List<T> = if (any(matches)) map { if (matches(it)) item else it } else this + item
}
