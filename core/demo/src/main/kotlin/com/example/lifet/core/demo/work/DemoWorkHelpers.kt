package com.example.lifet.core.demo.work

import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkShift
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Instante a la hora local [time] de [date] en [zone]. */
internal fun instantAt(
    date: LocalDate,
    time: LocalTime,
    zone: ZoneId,
): Instant = date.atTime(time).atZone(zone).toInstant()

/** Jornada que empieza a [start] y acaba [durationSec] después (abierta si [durationSec] es `null`). */
internal fun demoShift(
    id: String,
    date: LocalDate,
    start: LocalTime,
    zone: ZoneId,
    durationSec: Long?,
    projectId: String? = null,
    breaks: List<WorkBreak> = emptyList(),
    note: String = "",
): WorkShift {
    val begin = instantAt(date, start, zone)
    return WorkShift(
        id = id,
        start = begin,
        zone = zone,
        end = durationSec?.let { begin.plusSeconds(it) },
        breaks = breaks,
        projectId = projectId,
        note = note,
    )
}

/** Pausa de [minutes] minutos que empieza [afterSec] segundos después de [shiftStart]. */
internal fun demoBreak(
    shiftStart: Instant,
    afterSec: Long,
    minutes: Long,
): WorkBreak {
    val begin = shiftStart.plusSeconds(afterSec)
    return WorkBreak(begin, begin.plusSeconds(minutes * SECONDS_PER_MINUTE))
}

internal const val SECONDS_PER_MINUTE = 60L
internal const val SECONDS_PER_HOUR = 3_600L

/** Repite [seed] hasta acercarse a [max] caracteres sin pasarse, y sin partir nunca un emoji por la mitad. */
internal fun fitText(
    seed: String,
    max: Int,
): String =
    buildString {
        while (length + seed.length <= max) append(seed)
    }

/**
 * Quita lo que aún no ha ocurrido respecto al reloj: una jornada que empieza en el futuro, o cuyo final queda por
 * delante de "ahora" (se queda abierta si es la de hoy). Las pausas se recortan igual. Los datos de demo nunca deben
 * adelantarse a "ahora".
 */
internal fun WorkDataSet.withoutFuture(clock: Clock): WorkDataSet {
    val now = clock.instant()
    return copy(
        shifts = shifts.filter { it.start <= now }.map { it.cutAt(now) },
        focusSessions = focusSessions.filter { it.start <= now },
        notes = notes.filter { it.date <= LocalDate.now(clock) },
    )
}

private fun WorkShift.cutAt(now: Instant): WorkShift {
    val finish = end
    if (finish == null || finish <= now) return this
    return copy(end = null, breaks = breaks.filter { it.start < now }.map { it.cutAt(now) })
}

private fun WorkBreak.cutAt(now: Instant): WorkBreak {
    val finish = end
    return if (finish != null && finish > now) copy(end = null) else this
}
