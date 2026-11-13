package com.example.lifet.feature.finance

import com.example.lifet.core.designsystem.util.parseAmount
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.FinanceLimits

/**
 * Importe tecleado en [currency], o `null` si no es válido o supera [FinanceLimits.MAX_AMOUNT_MINOR].
 * Todo importe de Finanzas debe pasar por aquí para que ninguna suma pueda desbordar un `Long`.
 */
internal fun parseFinanceAmount(
    text: String,
    currency: CurrencyCode,
): Money? = parseAmount(text, currency)?.takeIf { it.amountMinor <= FinanceLimits.MAX_AMOUNT_MINOR }
