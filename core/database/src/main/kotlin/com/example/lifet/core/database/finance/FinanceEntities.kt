package com.example.lifet.core.database.finance

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.Frequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Importes como Long en unidades menores + código de divisa: nunca Double.

@Entity(tableName = "finance_accounts")
public data class FinanceAccountEntity(
    @PrimaryKey public val id: String,
    public val name: String,
    public val type: AccountType,
    public val openingMinor: Long,
    public val currency: String,
    public val archived: Boolean,
    public val sortOrder: Int,
)

@Entity(tableName = "finance_categories")
public data class FinanceCategoryEntity(
    @PrimaryKey public val id: String,
    public val kind: CategoryKind,
    public val iconKey: String,
    public val customName: String?,
    public val defaultCategory: DefaultCategory?,
    public val archived: Boolean,
    public val sortOrder: Int,
)

@Entity(
    tableName = "finance_transactions",
    indices = [Index("date"), Index("accountId"), Index("categoryId"), Index("recurringRuleId")],
)
public data class FinanceTransactionEntity(
    @PrimaryKey public val id: String,
    public val accountId: String,
    public val categoryId: String,
    public val amountMinor: Long,
    public val currency: String,
    public val date: LocalDate,
    public val note: String,
    public val recurringRuleId: String?,
    public val createdAt: Instant,
    public val zoneId: ZoneId,
)

@Entity(tableName = "finance_transfers", indices = [Index("date")])
public data class FinanceTransferEntity(
    @PrimaryKey public val id: String,
    public val fromAccountId: String,
    public val toAccountId: String,
    public val amountMinor: Long,
    public val currency: String,
    public val date: LocalDate,
    public val note: String,
    public val createdAt: Instant,
    public val zoneId: ZoneId,
)

@Entity(tableName = "finance_budgets")
public data class FinanceBudgetEntity(
    @PrimaryKey public val categoryId: String,
    public val limitMinor: Long,
    public val currency: String,
)

@Entity(tableName = "finance_recurring_rules")
public data class FinanceRecurringRuleEntity(
    @PrimaryKey public val id: String,
    public val name: String,
    public val amountMinor: Long,
    public val currency: String,
    public val categoryId: String,
    public val accountId: String,
    public val frequency: Frequency,
    public val startDate: LocalDate,
    public val endDate: LocalDate?,
    public val active: Boolean,
    public val generatedThrough: LocalDate?,
)
