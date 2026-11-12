package com.example.lifet.core.model.finance

import com.example.lifet.core.model.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Movimiento. [amount] lleva signo: gasto negativo, ingreso positivo. Una devolución es positiva sobre una
 * categoría de gasto, de modo que reduce el gasto de esa categoría sin tratarse como ingreso.
 *
 * [date] es el día lógico en [zone]; [createdAt] el instante real del alta. No hay estado "previsto"
 * almacenado: un movimiento es previsto mientras su fecha sea posterior a hoy ([isPlannedAt]), así nunca
 * queda desfasado si la app pasa días cerrada.
 */
public data class FinanceTransaction(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val amount: Money,
    val date: LocalDate,
    val note: String = "",
    val recurringRuleId: String? = null,
    val createdAt: Instant,
    val zone: ZoneId,
) {
    public fun isPlannedAt(today: LocalDate): Boolean = date > today
}

/** Transferencia entre dos cuentas de la misma divisa. No es gasto ni ingreso. [amount] es positivo. */
public data class FinanceTransfer(
    val id: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Money,
    val date: LocalDate,
    val note: String = "",
    val createdAt: Instant,
    val zone: ZoneId,
)

/** Límite mensual de gasto de una categoría. */
public data class Budget(
    val categoryId: String,
    val limit: Money,
)

/** Todo el contenido de finanzas; sirve para calcular informes, sembrar datos de demo, importar y exportar. */
public data class FinanceDataSet(
    val accounts: List<Account> = emptyList(),
    val categories: List<FinanceCategory> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val transfers: List<FinanceTransfer> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val rules: List<RecurringRule> = emptyList(),
)
