package com.example.lifet.core.model.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money

/** Tipo de cuenta; sólo cambia el icono y la agrupación, no las reglas de cálculo. */
public enum class AccountType { CASH, BANK, CARD, SAVINGS }

/**
 * Cuenta donde viven los movimientos. El saldo actual es [openingBalance] más los movimientos confirmados
 * y las transferencias; nunca se guarda, siempre se calcula. Una cuenta con historial se archiva, no se borra.
 */
public data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
) {
    public val currency: CurrencyCode get() = openingBalance.currency
}

/** Cuenta con su saldo calculado. */
public data class AccountBalance(
    val account: Account,
    val balance: Money,
)
