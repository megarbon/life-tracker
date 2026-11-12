package com.example.lifet.core.data.finance

import android.database.SQLException
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.finance.FinanceDao
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.UserPreferences
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.Budget
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.RecurringRule
import com.example.lifet.core.model.finance.defaultFinanceCategories
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@Suppress("TooManyFunctions") // Implementa FinanceRepository.
internal class RoomFinanceRepository
    @Inject
    constructor(
        private val dao: FinanceDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : FinanceRepository {
        override fun observeData(): Flow<FinanceDataSet> =
            combine(
                dao.observeAccounts(),
                dao.observeCategories(),
                dao.observeTransactions(),
                dao.observeTransfers(),
                combine(dao.observeBudgets(), dao.observeRules(), ::Pair),
            ) { accounts, categories, transactions, transfers, (budgets, rules) ->
                FinanceDataSet(
                    accounts = accounts.map { it.toDomain() },
                    categories = categories.map { it.toDomain() },
                    transactions = transactions.map { it.toDomain() },
                    transfers = transfers.map { it.toDomain() },
                    budgets = budgets.map { it.toDomain() },
                    rules = rules.map { it.toDomain() },
                )
            }.flowOn(ioDispatcher)

        override suspend fun saveTransaction(transaction: FinanceTransaction): AppResult<Unit> =
            write { dao.upsertTransaction(transaction.toEntity()) }

        override suspend fun deleteTransaction(id: String): AppResult<Unit> = write { dao.deleteTransaction(id) }

        override suspend fun saveTransfer(transfer: FinanceTransfer): AppResult<Unit> =
            write { dao.upsertTransfer(transfer.toEntity()) }

        override suspend fun deleteTransfer(id: String): AppResult<Unit> = write { dao.deleteTransfer(id) }

        override suspend fun saveAccount(account: Account): AppResult<Unit> =
            write { dao.upsertAccount(account.toEntity()) }

        override suspend fun saveCategory(category: FinanceCategory): AppResult<Unit> =
            write { dao.upsertCategory(category.toEntity()) }

        override suspend fun setBudget(
            categoryId: String,
            limit: Money?,
        ): AppResult<Unit> =
            write {
                if (limit ==
                    null
                ) {
                    dao.deleteBudget(categoryId)
                } else {
                    dao.upsertBudget(Budget(categoryId, limit).toEntity())
                }
            }

        override suspend fun saveRule(
            rule: RecurringRule,
            today: LocalDate,
        ): AppResult<Unit> = write { dao.saveRule(rule.toEntity(), today) }

        override suspend fun deleteRule(
            id: String,
            today: LocalDate,
        ): AppResult<Unit> = write { dao.deleteRule(id, today) }

        override suspend fun ensureDefaults(): AppResult<Unit> =
            write {
                if (dao.categoryCount() == 0) {
                    defaultFinanceCategories().forEach { dao.upsertCategory(it.toEntity()) }
                }
                if (dao.accountCount() == 0) {
                    val currency = UserPreferences().currency
                    dao.upsertAccount(
                        Account(Account.DEFAULT_ID, "", AccountType.BANK, Money.zero(currency)).toEntity(),
                    )
                }
            }

        override suspend fun applyGenerated(
            transactions: List<FinanceTransaction>,
            ruleIds: List<String>,
            through: LocalDate,
        ): AppResult<Unit> = write { dao.applyGenerated(transactions.map { it.toEntity() }, ruleIds, through) }

        override suspend fun replaceAll(data: FinanceDataSet): AppResult<Unit> =
            write {
                dao.replaceAll(
                    accounts = data.accounts.map { it.toEntity() },
                    categories = data.categories.map { it.toEntity() },
                    transactions = data.transactions.map { it.toEntity() },
                    transfers = data.transfers.map { it.toEntity() },
                    budgets = data.budgets.map { it.toEntity() },
                    rules = data.rules.map { it.toEntity() },
                )
            }

        private suspend fun write(block: suspend () -> Unit): AppResult<Unit> =
            withContext(ioDispatcher) {
                runCatchingApp(onError = { if (it is SQLException) AppError.Storage else AppError.Unknown }) { block() }
            }
    }
