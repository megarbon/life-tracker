package com.example.lifet.core.designsystem.util

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val DEFAULT_FRACTION_DIGITS = 2

/** Decimales de la divisa (EUR=2, JPY=0). Divisas desconocidas para la JVM usan 2. */
public fun CurrencyCode.fractionDigits(): Int =
    runCatching { Currency.getInstance(value).defaultFractionDigits }
        .getOrDefault(DEFAULT_FRACTION_DIGITS)
        .coerceAtLeast(0)

/** Formatea con símbolo y separadores del [locale]; el signo negativo se conserva. */
public fun Money.format(locale: Locale = Locale.getDefault()): String {
    val digits = currency.fractionDigits()
    val format =
        NumberFormat.getCurrencyInstance(locale).apply {
            runCatching { this.currency = Currency.getInstance(this@format.currency.value) }
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
    return format.format(BigDecimal(amountMinor).movePointLeft(digits))
}

/**
 * Convierte texto tecleado ("12,5", "12.50", "1 234,5") en [Money] positivo. Devuelve `null` si no es
 * un número válido o si tiene más decimales de los que admite la divisa; nunca redondea en silencio.
 */
public fun parseAmount(
    text: String,
    currency: CurrencyCode,
): Money? {
    val cleaned = text.trim().replace(" ", "").replace(',', '.')
    val decimal = cleaned.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
    val digits = currency.fractionDigits()
    val fits = decimal != null && decimal.signum() >= 0 && decimal.stripTrailingZeros().scale() <= digits
    return decimal.takeIf { fits }?.let {
        runCatching { it.setScale(digits).movePointRight(digits).longValueExact() }
            .getOrNull()
            ?.let { minor -> Money(minor, currency) }
    }
}

/** Texto editable para un importe ya guardado (sin símbolo ni separador de miles). */
public fun Money.toEditableText(): String {
    val digits = currency.fractionDigits()
    return BigDecimal(amountMinor).movePointLeft(digits).abs().toPlainString()
}

/** Normaliza lo que teclea el usuario: sólo dígitos y un separador decimal, limitado a los decimales de la divisa. */
public fun sanitizeAmountInput(
    raw: String,
    currency: CurrencyCode,
): String {
    val maxDecimals = currency.fractionDigits()
    val builder = StringBuilder()
    var separatorSeen = false
    var decimals = 0
    for (char in raw) {
        val isSeparator = char == ',' || char == '.'
        when {
            char.isDigit() && !separatorSeen -> {
                builder.append(char)
            }

            char.isDigit() && decimals < maxDecimals -> {
                builder.append(char)
                decimals++
            }

            // Sin decimales (JPY) el separador corta la entrada: "500,5" es 500, no 5005.
            isSeparator && !separatorSeen -> {
                if (maxDecimals > 0) builder.append(char)
                separatorSeen = true
            }
        }
    }
    return builder.toString()
}
