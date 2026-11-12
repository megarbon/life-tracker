package com.example.lifet.core.model.finance

import com.example.lifet.core.model.Money

/**
 * Lo que el usuario elige al registrar un movimiento. La devolución es un importe positivo sobre una categoría
 * de gasto: reduce el gasto de esa categoría y no cuenta como ingreso.
 */
public enum class EntryType(
    public val categoryKind: CategoryKind,
    private val negative: Boolean,
) {
    EXPENSE(CategoryKind.EXPENSE, negative = true),
    INCOME(CategoryKind.INCOME, negative = false),
    REFUND(CategoryKind.EXPENSE, negative = false),
    ;

    /** Convierte el importe tecleado (positivo) en el importe con signo que se guarda. */
    public fun signed(typed: Money): Money = if (negative) -typed else typed

    public companion object {
        /** Tipo de un movimiento ya guardado, a partir del signo y del tipo de su categoría. */
        public fun of(
            amount: Money,
            categoryKind: CategoryKind?,
        ): EntryType =
            when {
                amount.isNegative -> EXPENSE
                categoryKind == CategoryKind.EXPENSE -> REFUND
                else -> INCOME
            }
    }
}
