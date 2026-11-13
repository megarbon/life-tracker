package com.example.lifet.feature.work

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.work.WorkRules
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formateadores dependientes del idioma, creados una vez por composición. Todo el tiempo llega como `Instant` o
 * segundos enteros: ningún `Double` toca un dato guardado.
 */
internal class WorkFormatters(
    val locale: Locale,
) {
    private val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val time: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    private val weekdayDayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", locale)
    private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    private val weekdayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", locale)
    private val shortDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M", locale)

    fun date(date: LocalDate): String = date.format(mediumDate)

    /** "Lunes, 9 mar" con la inicial en mayúscula según el idioma. */
    fun dayTitle(date: LocalDate): String = date.format(weekdayDayMonth).replaceFirstChar { it.titlecase(locale) }

    fun shortWeekday(date: LocalDate): String = date.format(weekdayFormat)

    /** "9/3": etiqueta corta del eje X de una gráfica. */
    fun shortDate(date: LocalDate): String = date.format(shortDateFormat)

    fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale)

    fun weekTitle(week: DateRange): String = "${week.start.format(dayMonth)} – ${week.endInclusive.format(dayMonth)}"

    /** Hora local de un instante en la zona en la que se registró. */
    fun clock(
        instant: Instant,
        zone: ZoneId,
    ): String = instant.atZone(zone).format(time)

    fun clock(minuteOfDay: Int): String =
        LocalTime.of(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR).format(time)

    /** `HH:MM:SS` del cronómetro de la jornada. */
    fun stopwatch(seconds: Long): String {
        val total = seconds.coerceAtLeast(0L)
        val hours = total / WorkRules.SECONDS_PER_HOUR
        val minutes = total % WorkRules.SECONDS_PER_HOUR / WorkRules.SECONDS_PER_MINUTE
        val rest = total % WorkRules.SECONDS_PER_MINUTE
        return String.format(locale, "%02d:%02d:%02d", hours, minutes, rest)
    }

    /** `MM:SS` de la cuenta atrás (con horas si el intervalo las tiene). */
    fun countdown(seconds: Long): String {
        val total = seconds.coerceAtLeast(0L)
        return if (total >= WorkRules.SECONDS_PER_HOUR) {
            stopwatch(total)
        } else {
            String.format(
                locale,
                "%02d:%02d",
                total / WorkRules.SECONDS_PER_MINUTE,
                total % WorkRules.SECONDS_PER_MINUTE,
            )
        }
    }
}

/**
 * Duraciones legibles ("7 h 32 min", "45 min", "3 h") a partir de plantillas de recursos. Los segundos se truncan a
 * minutos: un cronómetro que muestra segundos ya los enseña por su lado.
 */
internal class DurationFormat(
    private val locale: Locale,
    private val hoursAndMinutes: String,
    private val hoursOnly: String,
    private val minutesOnly: String,
    private val plus: String,
    private val minus: String,
) {
    fun format(seconds: Long): String {
        val totalMinutes = seconds.coerceAtLeast(0L) / WorkRules.SECONDS_PER_MINUTE
        val hours = (totalMinutes / MINUTES_IN_HOUR).toInt()
        val minutes = (totalMinutes % MINUTES_IN_HOUR).toInt()
        return when {
            hours == 0 -> minutesOnly.format(locale, minutes)
            minutes == 0 -> hoursOnly.format(locale, hours)
            else -> hoursAndMinutes.format(locale, hours, minutes)
        }
    }

    /** Con signo explícito ("+1 h 20 min", "−45 min"): el sentido nunca depende sólo del color. */
    fun signed(seconds: Long): String =
        when {
            seconds / WorkRules.SECONDS_PER_MINUTE > 0L -> plus.format(locale, format(seconds))
            seconds / WorkRules.SECONDS_PER_MINUTE < 0L -> minus.format(locale, format(-seconds))
            else -> format(0L)
        }

    /** Horas con decimales para las gráficas (un dato derivado de sólo lectura, nunca se guarda). */
    fun chartHours(seconds: Long): Double = seconds.toDouble() / WorkRules.SECONDS_PER_HOUR
}

@Composable
internal fun rememberWorkFormatters(): WorkFormatters {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) { WorkFormatters(locale) }
}

@Composable
internal fun rememberDurationFormat(): DurationFormat {
    val locale = LocalLocale.current.platformLocale
    val hm = stringResource(R.string.work_duration_hm)
    val h = stringResource(R.string.work_duration_h)
    val m = stringResource(R.string.work_duration_m)
    val plus = stringResource(R.string.work_balance_plus)
    val minus = stringResource(R.string.work_balance_minus)
    return remember(locale, hm, h, m, plus, minus) { DurationFormat(locale, hm, h, m, plus, minus) }
}
