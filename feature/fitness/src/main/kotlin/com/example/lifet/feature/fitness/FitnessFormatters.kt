package com.example.lifet.feature.fitness

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import com.example.lifet.core.model.fitness.FitnessRules
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val ONE_DECIMAL = 1
private const val TWO_DECIMALS = 2

/**
 * Formateadores dependientes del idioma, creados una vez por composición. Pesos y distancias llegan en gramos y
 * metros (enteros) y se convierten con `BigDecimal`: ningún `Double` toca un dato guardado.
 */
internal class FitnessFormatters(
    val locale: Locale,
) {
    private val oneDecimal = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = ONE_DECIMAL }
    private val twoDecimals = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = TWO_DECIMALS }
    private val whole = NumberFormat.getIntegerInstance(locale)

    val fullDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M", locale)
    val time: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    fun number(value: Int): String = whole.format(value)

    fun number(value: Long): String = whole.format(value)

    /** Kilos con un decimal como mucho ("82,4", "82"). */
    fun kilos(grams: Long): String =
        oneDecimal.format(BigDecimal(grams).divide(KILO, ONE_DECIMAL, RoundingMode.HALF_UP))

    /** Kilos con signo explícito para variaciones ("+0,4", "-1,2"): el sentido no depende sólo del color. */
    fun signedKilos(grams: Long): String = (if (grams > 0) "+" else "") + kilos(grams)

    /** Carga de una serie: hasta dos decimales, porque las cargas de gimnasio llevan medios kilos ("62,5"). */
    fun load(grams: Long): String =
        twoDecimals.format(BigDecimal(grams).divide(KILO, TWO_DECIMALS, RoundingMode.HALF_UP))

    fun kilometers(meters: Long): String =
        twoDecimals.format(BigDecimal(meters).divide(KILO, TWO_DECIMALS, RoundingMode.HALF_UP))

    fun date(date: LocalDate): String = date.format(mediumDate)

    /** "Marzo 2026" con la inicial en mayúscula según el idioma. */
    fun monthTitle(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

    fun clock(minuteOfDay: Int): String =
        LocalTime.of(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR).format(time)

    /** Minutos redondeados (nunca por debajo de uno) de una duración en segundos. */
    fun minutes(seconds: Long): Int =
        ((seconds + FitnessRules.SECONDS_PER_MINUTE / 2) / FitnessRules.SECONDS_PER_MINUTE).toInt().coerceAtLeast(1)

    private companion object {
        val KILO: BigDecimal = BigDecimal(FitnessRules.GRAMS_PER_KILO)
        const val MINUTES_IN_HOUR = 60
    }
}

@Composable
internal fun rememberFitnessFormatters(): FitnessFormatters {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) { FitnessFormatters(locale) }
}

/** Plural con un solo argumento, que es la cantidad: `quantity(R.plurals.x, 3)` -> "3 series". */
@Composable
internal fun quantity(
    @PluralsRes id: Int,
    count: Int,
): String = pluralStringResource(id, count, count)
