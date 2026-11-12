@file:Suppress("TooManyFunctions") // Un par de mapeos entity <-> dominio por cada una de las seis tablas.

package com.example.lifet.core.data.finance

import com.example.lifet.core.database.finance.FinanceAccountEntity
import com.example.lifet.core.database.finance.FinanceBudgetEntity
import com.example.lifet.core.database.finance.FinanceCategoryEntity
import com.example.lifet.core.database.finance.FinanceRecurringRuleEntity
import com.example.lifet.core.database.finance.FinanceTransactionEntity
import com.example.lifet.core.database.finance.FinanceTransferEntity
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.Budget
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.RecurringRule

internal fun FinanceAccountEntity.toDomain(): Account =
    Account(id, name, type, Money(openingMinor, CurrencyCode(currency)), archived, sortOrder)

internal fun Account.toEntity(): FinanceAccountEntity =
    FinanceAccountEntity(
        id = id,
        name = name,
        type = type,
        openingMinor = openingBalance.amountMinor,
        currency = currency.value,
        archived = archived,
        sortOrder = sortOrder,
    )

internal fun FinanceCategoryEntity.toDomain(): FinanceCategory =
    FinanceCategory(id, kind, iconKey, customName, defaultCategory, archived, sortOrder)

internal fun FinanceCategory.toEntity(): FinanceCategoryEntity =
    FinanceCategoryEntity(id, kind, iconKey, customName, defaultCategory, archived, sortOrder)

internal fun FinanceTransactionEntity.toDomain(): FinanceTransaction =
    FinanceTransaction(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amount = Money(amountMinor, CurrencyCode(currency)),
        date = date,
        note = note,
        recurringRuleId = recurringRuleId,
        createdAt = createdAt,
        zone = zoneId,
    )

internal fun FinanceTransaction.toEntity(): FinanceTransactionEntity =
    FinanceTransactionEntity(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amountMinor = amount.amountMinor,
        currency = amount.currency.value,
        date = date,
        note = note,
        recurringRuleId = recurringRuleId,
        createdAt = createdAt,
        zoneId = zone,
    )

internal fun FinanceTransferEntity.toDomain(): FinanceTransfer =
    FinanceTransfer(
        id = id,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        amount = Money(amountMinor, CurrencyCode(currency)),
        date = date,
        note = note,
        createdAt = createdAt,
        zone = zoneId,
    )

internal fun FinanceTransfer.toEntity(): FinanceTransferEntity =
    FinanceTransferEntity(
        id = id,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        amountMinor = amount.amountMinor,
        currency = amount.currency.value,
        date = date,
        note = note,
        createdAt = createdAt,
        zoneId = zone,
    )

internal fun FinanceBudgetEntity.toDomain(): Budget = Budget(categoryId, Money(limitMinor, CurrencyCode(currency)))

internal fun Budget.toEntity(): FinanceBudgetEntity =
    FinanceBudgetEntity(categoryId, limit.amountMinor, limit.currency.value)

internal fun FinanceRecurringRuleEntity.toDomain(): RecurringRule =
    RecurringRule(
        id = id,
        name = name,
        amount = Money(amountMinor, CurrencyCode(currency)),
        categoryId = categoryId,
        accountId = accountId,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        active = active,
        generatedThrough = generatedThrough,
    )

internal fun RecurringRule.toEntity(): FinanceRecurringRuleEntity =
    FinanceRecurringRuleEntity(
        id = id,
        name = name,
        amountMinor = amount.amountMinor,
        currency = amount.currency.value,
        categoryId = categoryId,
        accountId = accountId,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        active = active,
        generatedThrough = generatedThrough,
    )
