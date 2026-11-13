package com.example.lifet.feature.quit

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val DIGITS = Regex("""^\d+$""")

private const val MINUTES_IN_HOUR = 60

/** Entero sin signo; `null` si el texto no son sólo dígitos o no cabe en un `Int`. */
internal fun parseWholeNumber(text: String): Int? = text.trim().takeIf { DIGITS.matches(it) }?.toIntOrNull()

/** Deja sólo dígitos ASCII, con como mucho [maxLength]: lo que teclea el usuario en un campo de cantidad. */
internal fun sanitizeDigits(
    raw: String,
    maxLength: Int,
): String = raw.filter { it in '0'..'9' }.take(maxLength)

/**
 * Instante de la hora local [minuteOfDay] de [date] en [zone]. En el salto de primavera (la hora no existe) java.time
 * adelanta al instante válido siguiente y en el de otoño (la hora existe dos veces) elige la primera.
 */
internal fun instantOf(
    date: LocalDate,
    minuteOfDay: Int,
    zone: ZoneId,
): Instant = date.atTime(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR).atZone(zone).toInstant()
