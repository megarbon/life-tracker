package com.example.lifet.feature.fitness

import java.math.BigDecimal

private val DECIMAL = Regex("""^(\d+([.,]\d*)?|[.,]\d+)$""")
private val DIGITS = Regex("""^\d+$""")

/**
 * Convierte texto tecleado ("82,5", "2.5", "1 234") en un entero con [scale] decimales implícitos: con scale = 3,
 * "82,5" son 82 500. Devuelve `null` si no es un número no negativo, si tiene más decimales de los que admite
 * [scale] (nunca redondea en silencio) o si no cabe en un `Long`. Sin `Double`: no se pierde precisión.
 */
internal fun parseScaled(
    text: String,
    scale: Int,
): Long? {
    val cleaned = text.trim().replace(" ", "")
    val decimal = cleaned.takeIf { DECIMAL.matches(it) }?.replace(',', '.')?.toBigDecimalOrNull()
    val fits = decimal != null && decimal.stripTrailingZeros().scale() <= scale
    return decimal.takeIf { fits }?.let {
        runCatching {
            it
                .stripTrailingZeros()
                .setScale(scale)
                .movePointRight(scale)
                .longValueExact()
        }.getOrNull()
    }
}

/** Entero sin signo; `null` si el texto no son sólo dígitos o no cabe en un `Int`. */
internal fun parseWholeNumber(text: String): Int? = text.trim().takeIf { DIGITS.matches(it) }?.toIntOrNull()

/** Texto editable de [value] con [scale] decimales implícitos, sin ceros sobrantes ni separador de miles. */
internal fun Long.toEditableText(scale: Int): String =
    BigDecimal(this).movePointLeft(scale).stripTrailingZeros().toPlainString()

/**
 * Normaliza lo que teclea el usuario en un campo decimal: sólo dígitos y un separador, con como mucho [maxDecimals]
 * decimales. Con [maxDecimals] a cero (enteros) no admite separador.
 */
internal fun sanitizeDecimalInput(
    raw: String,
    maxDecimals: Int,
): String {
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

            isSeparator && !separatorSeen && maxDecimals > 0 -> {
                builder.append(char)
                separatorSeen = true
            }
        }
    }
    return builder.toString()
}
