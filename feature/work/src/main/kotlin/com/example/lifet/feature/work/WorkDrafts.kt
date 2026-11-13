package com.example.lifet.feature.work

import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.ShiftIssue
import com.example.lifet.core.model.work.ShiftValidator
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/*
 * Formularios de la UI. Son `Serializable` para que `rememberSaveable` los conserve tras una rotación. Las horas se
 * eligen con selectores, así que el borrador siempre se puede convertir en dominio; las reglas (salida anterior a la
 * entrada, pausas solapadas, solape con otras jornadas) las decide `ShiftValidator`, que es la única fuente de verdad.
 */

internal const val MINUTES_IN_HOUR = 60
internal const val MINUTES_IN_DAY = 24 * MINUTES_IN_HOUR

/** Una pausa tecleada con horas del día. Sin [endMinute] está en curso. El día se deduce de la jornada. */
internal data class BreakDraft(
    val startMinute: Int,
    val endMinute: Int? = null,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Los instantes con los que se abrió una jornada al editarla, en milisegundos (un `Long` sí es `Serializable`) junto
 * con las horas del día de las que salieron. Sirven para no degradar una jornada: si el usuario no toca una hora, se
 * conserva el instante original con sus segundos en lugar de redondear al minuto.
 */
internal data class ShiftOrigin(
    val startMillis: Long,
    val startMinute: Int,
    val startDate: LocalDate,
    val endMillis: Long?,
    val endMinute: Int,
    val breaks: List<BreakOrigin>,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** Una pausa tal como estaba al abrir la jornada. */
internal data class BreakOrigin(
    val startMillis: Long,
    val endMillis: Long?,
    val startMinute: Int,
    val endMinute: Int?,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Borrador de una jornada (alta a mano y edición). La salida se guarda como hora del día: si es igual o anterior a la
 * entrada, termina al día siguiente (una jornada nocturna, o de 24 h exactas). Las pausas se colocan en el primer
 * momento posible desde la entrada, así que no hace falta pedir fechas para cada una.
 *
 * Al editar, [origin] recuerda los instantes originales: lo que no se toque se conserva tal cual (con sus segundos y
 * sin que la salida salte de día), y sólo se reconstruye con el selector de hora lo que el usuario cambie.
 */
internal data class ShiftDraft(
    val id: String,
    val startDate: LocalDate,
    val startMinute: Int,
    val hasEnd: Boolean = true,
    val endMinute: Int = 0,
    val projectId: String? = null,
    val note: String = "",
    val breaks: List<BreakDraft> = emptyList(),
    val origin: ShiftOrigin? = null,
) : Serializable {
    /** La salida cae al día siguiente de la entrada. */
    val endsNextDay: Boolean get() = hasEnd && endMinute <= startMinute && !keepsOriginalEnd

    private val keepsOriginalStart: Boolean
        get() = origin != null && origin.startDate == startDate && origin.startMinute == startMinute

    private val keepsOriginalEnd: Boolean
        get() = keepsOriginalStart && origin?.endMillis != null && origin.endMinute == endMinute

    fun toShift(zone: ZoneId): WorkShift {
        val start =
            origin?.takeIf { keepsOriginalStart }?.let { Instant.ofEpochMilli(it.startMillis) }
                ?: instantAt(startDate, startMinute, zone)
        val end =
            when {
                !hasEnd -> null
                keepsOriginalEnd -> origin?.endMillis?.let(Instant::ofEpochMilli)
                else -> instantAt(startDate.plusDays(if (endsNextDay) 1 else 0), endMinute, zone)
            }
        return WorkShift(
            id = id,
            start = start,
            zone = zone,
            end = end,
            breaks = breaks.map { it.resolve(start, zone) },
            projectId = projectId,
            note = note.trim(),
        )
    }

    /** Problemas de la jornada que el borrador describe, sin conocer las demás jornadas. */
    fun issues(
        zone: ZoneId,
        now: Instant,
    ): Set<ShiftIssue> = ShiftValidator.validate(toShift(zone), emptyList(), now)

    /** Una pausa que el usuario no ha tocado conserva su instante original (la jornada no cambió de inicio). */
    private fun BreakDraft.resolve(
        shiftStart: Instant,
        zone: ZoneId,
    ): WorkBreak {
        val kept = origin?.breaks?.firstOrNull { it.startMinute == startMinute && it.endMinute == endMinute }
        if (kept != null && keepsOriginalStart) {
            return WorkBreak(Instant.ofEpochMilli(kept.startMillis), kept.endMillis?.let(Instant::ofEpochMilli))
        }
        val first = nextOccurrence(shiftStart, startMinute, zone)
        val last = endMinute?.let { nextOccurrence(first.plusSeconds(1), it, zone) }
        return WorkBreak(first, last)
    }

    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** El primer instante igual o posterior a [after] cuya hora local es [minuteOfDay]. */
internal fun nextOccurrence(
    after: Instant,
    minuteOfDay: Int,
    zone: ZoneId,
): Instant {
    val local = after.atZone(zone).toLocalDate()
    val today = instantAt(local, minuteOfDay, zone)
    return if (today >= after) today else instantAt(local.plusDays(1), minuteOfDay, zone)
}

internal fun instantAt(
    date: LocalDate,
    minuteOfDay: Int,
    zone: ZoneId,
): Instant =
    date.atTime(LocalTime.of(minuteOfDay / MINUTES_IN_HOUR, minuteOfDay % MINUTES_IN_HOUR)).atZone(zone).toInstant()

internal fun Instant.minuteOfDay(zone: ZoneId): Int {
    val time = atZone(zone).toLocalTime()
    return time.hour * MINUTES_IN_HOUR + time.minute
}

/** Borrador de una jornada nueva a mano: hoy, de [startMinute] a [endMinute]. */
internal fun blankShiftDraft(
    id: String,
    today: LocalDate,
    startMinute: Int = DEFAULT_START_MINUTE,
    endMinute: Int = DEFAULT_END_MINUTE,
): ShiftDraft = ShiftDraft(id = id, startDate = today, startMinute = startMinute, endMinute = endMinute)

internal fun WorkShift.toDraft(): ShiftDraft {
    val startMinute = start.minuteOfDay(zone)
    val endMinute = end?.minuteOfDay(zone) ?: 0
    return ShiftDraft(
        id = id,
        startDate = startDate,
        startMinute = startMinute,
        hasEnd = end != null,
        endMinute = endMinute,
        projectId = projectId,
        note = note,
        breaks = breaks.map { BreakDraft(it.start.minuteOfDay(zone), it.end?.minuteOfDay(zone)) },
        origin =
            ShiftOrigin(
                startMillis = start.toEpochMilli(),
                startMinute = startMinute,
                startDate = startDate,
                endMillis = end?.toEpochMilli(),
                endMinute = endMinute,
                breaks =
                    breaks.map {
                        BreakOrigin(
                            startMillis = it.start.toEpochMilli(),
                            endMillis = it.end?.toEpochMilli(),
                            startMinute = it.start.minuteOfDay(zone),
                            endMinute = it.end?.minuteOfDay(zone),
                        )
                    },
            ),
    )
}

private const val DEFAULT_START_MINUTE = 9 * MINUTES_IN_HOUR
private const val DEFAULT_END_MINUTE = 17 * MINUTES_IN_HOUR

/** Borrador de un proyecto. */
internal data class ProjectDraft(
    val id: String?,
    val name: String = "",
    val color: ProjectColor = ProjectColor.BLUE,
) : Serializable {
    val isValid: Boolean get() = name.isNotBlank()

    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** Ajustes de horas: objetivo semanal en horas con un decimal como mucho ("37,5") y días laborables. */
internal data class SettingsDraft(
    val hoursText: String,
    val workingDays: Set<DayOfWeek>,
) : Serializable {
    /** Objetivo en minutos, o `null` si el texto no es un número razonable. */
    val targetMinutes: Int?
        get() = parseHoursToMinutes(hoursText)?.takeIf { it in 1..WorkRules.MAX_WEEKLY_TARGET_MINUTES }

    val isValid: Boolean get() = targetMinutes != null && workingDays.isNotEmpty()

    fun toSettings(): WorkSettings? = targetMinutes?.let { WorkSettings(it, workingDays) }

    /** Añade el día si no estaba y lo quita si estaba. */
    fun withDayToggled(day: DayOfWeek): SettingsDraft =
        copy(workingDays = if (day in workingDays) workingDays - day else workingDays + day)

    private companion object {
        private const val serialVersionUID = 1L
    }
}

internal fun WorkSettings.toDraft(): SettingsDraft = SettingsDraft(minutesToHoursText(weeklyTargetMinutes), workingDays)

/** "37,5" o "37.5" -> 2.250 minutos. Más de un decimal o texto no numérico -> `null` (nunca se redondea). */
internal fun parseHoursToMinutes(text: String): Int? {
    val normalized = text.trim().replace(',', '.')
    val hours = normalized.takeIf { it.length <= MAX_HOURS_TEXT }?.toBigDecimalOrNull()
    val exact = hours != null && hours.scale() <= 1 && hours.signum() > 0 && hours <= MAX_WEEKLY_HOURS
    return if (exact) {
        hours
            .multiply(
                BigDecimal(MINUTES_IN_HOUR),
            ).setScale(0, RoundingMode.UNNECESSARY)
            .toInt()
    } else {
        null
    }
}

/** Acota el texto antes de convertirlo: así un pegado enorme nunca llega a `BigDecimal`. */
private const val MAX_HOURS_TEXT = 6
private val MAX_WEEKLY_HOURS = BigDecimal(WorkRules.MAX_WEEKLY_TARGET_MINUTES / MINUTES_IN_HOUR)

/** 2.250 -> "37,5"; 2.400 -> "40". Sólo como texto editable (la coma decimal depende del idioma). */
internal fun minutesToHoursText(minutes: Int): String {
    val hours = BigDecimal(minutes).divide(BigDecimal(MINUTES_IN_HOUR), 1, RoundingMode.HALF_UP)
    return hours.stripTrailingZeros().toPlainString()
}
