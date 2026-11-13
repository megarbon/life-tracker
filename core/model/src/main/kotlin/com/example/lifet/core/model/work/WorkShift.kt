package com.example.lifet.core.model.work

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Una pausa dentro de una jornada. Sin [end] está en curso. */
public data class WorkBreak(
    public val start: Instant,
    public val end: Instant? = null,
) {
    public val isOpen: Boolean get() = end == null

    /** Segundos de la pausa que caen dentro de `[from, until]`, para que una pausa mal colocada no reste de más. */
    internal fun secondsWithin(
        from: Instant,
        until: Instant,
    ): Long {
        val begin = maxOf(start, from)
        val finish = minOf(end ?: until, until)
        return if (finish > begin) Duration.between(begin, finish).seconds else 0L
    }
}

/**
 * Una jornada: del instante de entrada a la salida, con sus pausas. Se guarda el instante (UTC) con la [zone] del
 * usuario en ese momento, así que el día lógico ([startDate]) no cambia si viaja y las duraciones usan tiempo real:
 * una jornada que cruza la medianoche o un cambio de hora (DST) dura lo que dura de verdad.
 *
 * Una jornada pertenece al día en que empezó. Sin [end] está abierta: en curso o, si pasó mucho tiempo, olvidada
 * ([isForgotten]) y pendiente de cerrar a posteriori.
 */
public data class WorkShift(
    public val id: String,
    public val start: Instant,
    public val zone: ZoneId,
    public val end: Instant? = null,
    public val breaks: List<WorkBreak> = emptyList(),
    public val projectId: String? = null,
    public val note: String = "",
) {
    /** Día lógico en el que empezó. */
    public val startDate: LocalDate get() = start.atZone(zone).toLocalDate()

    public val isOpen: Boolean get() = end == null

    public val openBreak: WorkBreak? get() = breaks.lastOrNull { it.isOpen }

    public val isOnBreak: Boolean get() = isOpen && openBreak != null

    /** Tiempo transcurrido entre la entrada y la salida (o [now] si sigue abierta), pausas incluidas. */
    public fun elapsedSecondsAt(now: Instant): Long {
        val finish = end ?: now
        return if (finish > start) Duration.between(start, finish).seconds else 0L
    }

    public fun breakSecondsAt(now: Instant): Long {
        val finish = end ?: now
        return if (finish > start) breaks.sumOf { it.secondsWithin(start, finish) } else 0L
    }

    /** Tiempo trabajado: lo transcurrido menos las pausas. */
    public fun netSecondsAt(now: Instant): Long = (elapsedSecondsAt(now) - breakSecondsAt(now)).coerceAtLeast(0L)

    /** Abierta desde hace más de [WorkRules.FORGOTTEN_AFTER_SECONDS]: casi seguro que se olvidó fichar la salida. */
    public fun isForgotten(now: Instant): Boolean = isOpen && elapsedSecondsAt(now) > WorkRules.FORGOTTEN_AFTER_SECONDS

    /** La jornada termina (o, si sigue abierta, va por) un día distinto del que empezó. */
    public fun crossesMidnightAt(now: Instant): Boolean = (end ?: now).atZone(zone).toLocalDate() != startDate

    /** Empieza una pausa; no hace nada si ya hay una abierta o la jornada está cerrada. */
    public fun withBreakStarted(at: Instant): WorkShift =
        if (!isOpen || openBreak != null) this else copy(breaks = breaks + WorkBreak(at))

    /** Cierra la pausa abierta, si la hay. */
    public fun withBreakEnded(at: Instant): WorkShift =
        if (openBreak == null) this else copy(breaks = breaks.map { if (it.isOpen) it.copy(end = at) else it })

    /**
     * Cierra la jornada en [at]. Las pausas que empezaban a esa hora o después se descartan y las que terminaban más
     * tarde (o seguían abiertas) se recortan a [at]: cerrar a posteriori una jornada olvidada nunca deja una pausa
     * fuera de su jornada.
     */
    public fun closedAt(at: Instant): WorkShift =
        copy(
            end = at,
            breaks =
                breaks
                    .filter { it.start < at }
                    .map { pause -> if (pause.end == null || pause.end > at) pause.copy(end = at) else pause },
        )
}
