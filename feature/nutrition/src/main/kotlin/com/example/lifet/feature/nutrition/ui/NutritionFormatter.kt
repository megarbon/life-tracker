package com.example.lifet.feature.nutrition.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private const val SMALL_VALUE = 10.0
private const val PERCENT = 100

/** Formatea cifras de nutrición según el idioma del dispositivo: miles con punto en es, con coma en en. */
internal class NutritionFormatter(
    locale: Locale,
) {
    private val integer = NumberFormat.getIntegerInstance(locale)
    private val oneDecimal =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }

    fun kcal(value: Double): String = integer.format(value.roundToLong())

    /** Gramos: enteros a partir de 10 g y con un decimal por debajo (0,3 g de proteína no debe verse como 0). */
    fun grams(value: Double): String =
        if (abs(value) >= SMALL_VALUE) integer.format(value.roundToLong()) else oneDecimal.format(value)

    fun amount(value: Int): String = integer.format(value)

    fun percent(fraction: Double): String = integer.format((fraction * PERCENT).roundToLong())
}

@Composable
internal fun rememberNutritionFormatter(): NutritionFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NutritionFormatter(locale) }
}

/** Acepta coma o punto decimal (teclado en español); rechaza vacío, texto, NaN e infinitos. */
internal fun parseDecimal(text: String): Double? =
    text
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() }
