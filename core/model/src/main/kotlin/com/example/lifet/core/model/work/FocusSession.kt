package com.example.lifet.core.model.work

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Tipo de intervalo del temporizador: sesión de foco o uno de los dos descansos del pomodoro. */
public enum class FocusKind(
    public val isFocus: Boolean,
    public val defaultMinutes: Int,
) {
    FOCUS(isFocus = true, defaultMinutes = WorkRules.DEFAULT_FOCUS_MINUTES),
    SHORT_BREAK(isFocus = false, defaultMinutes = WorkRules.SHORT_BREAK_MINUTES),
    LONG_BREAK(isFocus = false, defaultMinutes = WorkRules.LONG_BREAK_MINUTES),
}

/**
 * Un intervalo del temporizador. Se guarda la hora de inicio ([start], `Instant`) y la duración prevista, nunca
 * "segundos que quedan": así el temporizador sobrevive a una rotación o a que el proceso muera, porque lo que
 * queda se recalcula siempre con `ahora - inicio`. Sin [end] está en marcha.
 *
 * @property completed llegó a su duración completa (frente a cancelada antes de tiempo).
 */
public data class FocusSession(
    public val id: String,
    public val kind: FocusKind,
    public val start: Instant,
    public val zone: ZoneId,
    public val plannedSeconds: Long,
    public val end: Instant? = null,
    public val completed: Boolean = false,
    public val projectId: String? = null,
    public val title: String = "",
) {
    /** Día lógico en el que empezó. */
    public val startDate: LocalDate get() = start.atZone(zone).toLocalDate()

    /** Instante en el que el temporizador llega a cero. */
    public val plannedEnd: Instant get() = start.plusSeconds(plannedSeconds)

    public val isRunning: Boolean get() = end == null

    /** Segundos transcurridos en [now], sin pasar de la duración prevista (un temporizador no cuenta de más). */
    public fun elapsedSecondsAt(now: Instant): Long {
        val finish = end ?: now
        val elapsed = if (finish > start) Duration.between(start, finish).seconds else 0L
        return elapsed.coerceAtMost(plannedSeconds)
    }

    public fun remainingSecondsAt(now: Instant): Long = (plannedSeconds - elapsedSecondsAt(now)).coerceAtLeast(0L)

    /** Progreso entre 0 y 1. */
    public fun progressAt(now: Instant): Float =
        if (plannedSeconds <= 0L) 1f else (elapsedSecondsAt(now).toFloat() / plannedSeconds).coerceIn(0f, 1f)

    /** En marcha y con el tiempo agotado: hay que darla por terminada. */
    public fun isDueAt(now: Instant): Boolean = isRunning && now >= plannedEnd
}
