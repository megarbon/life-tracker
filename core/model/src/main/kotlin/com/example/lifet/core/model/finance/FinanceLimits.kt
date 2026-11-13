package com.example.lifet.core.model.finance

/**
 * Límites de entrada de Finanzas.
 *
 * Un importe sin tope puede desbordar un `Long` al sumar varios movimientos y tumbar los cálculos de todo el
 * área. 10^12 unidades menores son 10.000 millones de euros: muy por encima de cualquier uso real, y lo bastante
 * bajo para que sumar millones de movimientos siga cabiendo en un `Long` (≈ 9,2·10^18).
 */
public object FinanceLimits {
    public const val MAX_AMOUNT_MINOR: Long = 1_000_000_000_000L
}
