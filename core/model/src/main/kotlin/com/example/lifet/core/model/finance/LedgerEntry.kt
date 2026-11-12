package com.example.lifet.core.model.finance

import java.time.Instant
import java.time.LocalDate

/** Fila del listado de movimientos: un movimiento o una transferencia. */
public sealed interface LedgerEntry {
    public val id: String
    public val date: LocalDate
    public val createdAt: Instant
}

public data class TransactionEntry(
    val transaction: FinanceTransaction,
) : LedgerEntry {
    override val id: String get() = transaction.id
    override val date: LocalDate get() = transaction.date
    override val createdAt: Instant get() = transaction.createdAt
}

public data class TransferEntry(
    val transfer: FinanceTransfer,
) : LedgerEntry {
    override val id: String get() = transfer.id
    override val date: LocalDate get() = transfer.date
    override val createdAt: Instant get() = transfer.createdAt
}
