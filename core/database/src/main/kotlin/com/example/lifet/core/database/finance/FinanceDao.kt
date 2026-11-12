package com.example.lifet.core.database.finance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Acceso a las tablas de finanzas. Es `abstract class` porque las operaciones compuestas van en
 * `@Transaction`. Las escrituras de generación usan IGNORE: un movimiento ya generado (aunque el usuario
 * lo haya editado) nunca se sobrescribe.
 */
@Dao
public abstract class FinanceDao {
    @Query("SELECT * FROM finance_accounts ORDER BY archived, sortOrder, name")
    public abstract fun observeAccounts(): Flow<List<FinanceAccountEntity>>

    @Query("SELECT * FROM finance_categories ORDER BY sortOrder")
    public abstract fun observeCategories(): Flow<List<FinanceCategoryEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY date DESC, createdAt DESC")
    public abstract fun observeTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transfers ORDER BY date DESC, createdAt DESC")
    public abstract fun observeTransfers(): Flow<List<FinanceTransferEntity>>

    @Query("SELECT * FROM finance_budgets")
    public abstract fun observeBudgets(): Flow<List<FinanceBudgetEntity>>

    @Query("SELECT * FROM finance_recurring_rules ORDER BY name")
    public abstract fun observeRules(): Flow<List<FinanceRecurringRuleEntity>>

    @Query("SELECT * FROM finance_recurring_rules")
    public abstract suspend fun rules(): List<FinanceRecurringRuleEntity>

    @Query("SELECT COUNT(*) FROM finance_categories")
    public abstract suspend fun categoryCount(): Int

    @Upsert
    public abstract suspend fun upsertAccount(entity: FinanceAccountEntity)

    @Upsert
    public abstract suspend fun upsertCategory(entity: FinanceCategoryEntity)

    @Upsert
    public abstract suspend fun upsertTransaction(entity: FinanceTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract suspend fun insertGeneratedTransactions(entities: List<FinanceTransactionEntity>)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    public abstract suspend fun deleteTransaction(id: String)

    @Upsert
    public abstract suspend fun upsertTransfer(entity: FinanceTransferEntity)

    @Query("DELETE FROM finance_transfers WHERE id = :id")
    public abstract suspend fun deleteTransfer(id: String)

    @Upsert
    public abstract suspend fun upsertBudget(entity: FinanceBudgetEntity)

    @Query("DELETE FROM finance_budgets WHERE categoryId = :categoryId")
    public abstract suspend fun deleteBudget(categoryId: String)

    @Query("UPDATE finance_recurring_rules SET generatedThrough = :through WHERE id = :id")
    public abstract suspend fun updateGeneratedThrough(
        id: String,
        through: LocalDate,
    )

    @Query("DELETE FROM finance_transactions WHERE recurringRuleId = :ruleId AND date > :after")
    public abstract suspend fun deleteGeneratedAfter(
        ruleId: String,
        after: LocalDate,
    )

    /**
     * Guarda una regla y descarta sus movimientos futuros generados: se regeneran con los datos nuevos.
     * Los ya confirmados (fecha <= [today]) se conservan tal cual.
     */
    @Transaction
    public open suspend fun saveRule(
        entity: FinanceRecurringRuleEntity,
        today: LocalDate,
    ) {
        deleteGeneratedAfter(entity.id, today)
        val generated = entity.generatedThrough
        upsertRule(entity.copy(generatedThrough = if (generated != null && generated > today) today else generated))
    }

    @Transaction
    public open suspend fun deleteRule(
        id: String,
        today: LocalDate,
    ) {
        deleteGeneratedAfter(id, today)
        deleteRuleRow(id)
    }

    @Upsert
    public abstract suspend fun upsertRule(entity: FinanceRecurringRuleEntity)

    @Query("DELETE FROM finance_recurring_rules WHERE id = :id")
    public abstract suspend fun deleteRuleRow(id: String)

    /** Sustituye todos los datos de finanzas (demo, "borrar todo"). */
    @Transaction
    public open suspend fun replaceAll(
        accounts: List<FinanceAccountEntity>,
        categories: List<FinanceCategoryEntity>,
        transactions: List<FinanceTransactionEntity>,
        transfers: List<FinanceTransferEntity>,
        budgets: List<FinanceBudgetEntity>,
        rules: List<FinanceRecurringRuleEntity>,
    ) {
        clearAll()
        accounts.forEach { upsertAccount(it) }
        categories.forEach { upsertCategory(it) }
        insertGeneratedTransactions(transactions)
        transfers.forEach { upsertTransfer(it) }
        budgets.forEach { upsertBudget(it) }
        rules.forEach { upsertRule(it) }
    }

    @Query("DELETE FROM finance_transactions")
    public abstract suspend fun deleteAllTransactions()

    @Query("DELETE FROM finance_transfers")
    public abstract suspend fun deleteAllTransfers()

    @Query("DELETE FROM finance_budgets")
    public abstract suspend fun deleteAllBudgets()

    @Query("DELETE FROM finance_recurring_rules")
    public abstract suspend fun deleteAllRules()

    @Query("DELETE FROM finance_accounts")
    public abstract suspend fun deleteAllAccounts()

    @Query("DELETE FROM finance_categories")
    public abstract suspend fun deleteAllCategories()

    private suspend fun clearAll() {
        deleteAllTransactions()
        deleteAllTransfers()
        deleteAllBudgets()
        deleteAllRules()
        deleteAllAccounts()
        deleteAllCategories()
    }
}
