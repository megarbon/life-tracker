package com.example.lifet.feature.quit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.util.format
import com.example.lifet.core.designsystem.util.fractionDigits
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.QuitRules
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val MINUTES_IN_HOUR = 60
private const val DAYS_PER_YEAR = 365L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_MINUTE = 60L

/** Formateadores dependientes del idioma, creados una vez por composición. Ningún `Double` toca un dato guardado. */
internal class QuitFormatters(
    val locale: Locale,
) {
    private val whole = NumberFormat.getIntegerInstance(locale)

    val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M", locale)
    val time: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    fun number(value: Int): String = whole.format(value)

    fun number(value: Long): String = whole.format(value)

    fun date(date: LocalDate): String = date.format(mediumDate)

    fun money(money: Money): String = money.format(locale)

    fun clock(minuteOfDay: Int): String =
        LocalTime.of(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR).format(time)

    /** Una hora en punto con el formato del idioma: "9:00" o "9:00 AM". */
    fun hour(hour: Int): String = LocalTime.of(hour.coerceIn(0, LAST_HOUR), 0).format(time)

    /** "Marzo 2026" con la inicial en mayúscula según el idioma. */
    fun monthTitle(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

    private companion object {
        const val LAST_HOUR = 23
    }
}

@Composable
internal fun rememberQuitFormatters(): QuitFormatters {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) { QuitFormatters(locale) }
}

/** Plural con un solo argumento, que es la cantidad: `quantity(R.plurals.x, 3)` -> "3 días". */
@Composable
internal fun quantity(
    id: Int,
    count: Int,
): String = pluralStringResource(id, count, count)

/**
 * Una duración en lenguaje natural con las dos unidades mayores que no sean cero: "34 días y 18 horas",
 * "5 horas y 3 minutos", "2 años y 110 días". Los años son de 365 días, igual que los hitos.
 */
@Composable
internal fun spanText(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val days = safe / QuitRules.SECONDS_PER_DAY
    val parts =
        listOf(
            Triple(R.plurals.quit_span_years, days / DAYS_PER_YEAR, 0),
            Triple(R.plurals.quit_span_days, days % DAYS_PER_YEAR, 1),
            Triple(R.plurals.quit_span_hours, safe % QuitRules.SECONDS_PER_DAY / SECONDS_PER_HOUR, 2),
            Triple(R.plurals.quit_span_minutes, safe % SECONDS_PER_HOUR / SECONDS_PER_MINUTE, 3),
        ).filter { it.second > 0 }.take(2)
    val texts = parts.map { (plural, amount, _) -> quantity(plural, amount.toInt()) }
    return when (texts.size) {
        0 -> stringResource(R.string.quit_span_less_than_minute)
        1 -> texts.first()
        else -> stringResource(R.string.quit_span_join, texts[0], texts[1])
    }
}

/**
 * Importe en unidades menores convertido a `Double` sólo para dibujar una gráfica. Con una cifra enorme el `Double`
 * pierde precisión: no es un problema porque nunca se vuelve a guardar.
 */
internal fun Money.toChartValue(): Double =
    BigDecimal.valueOf(amountMinor).movePointLeft(currency.fractionDigits()).toDouble()

/** Vuelve a [Money] el valor de una gráfica (el inverso de [toChartValue]), saturando en `Long.MAX_VALUE`. */
internal fun Double.toChartMoney(currency: CurrencyCode): Money {
    val minor =
        BigDecimal.valueOf(this).movePointRight(currency.fractionDigits()).setScale(0, RoundingMode.HALF_UP)
    return Money(runCatching { minor.longValueExact() }.getOrDefault(Long.MAX_VALUE), currency)
}
