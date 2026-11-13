package com.example.lifet.core.model.work

import java.time.Duration
import java.time.Instant

/** Motivos por los que una jornada no se puede guardar. Identificadores estables, no texto de UI. */
public enum class ShiftIssue {
    START_IN_FUTURE,
    END_IN_FUTURE,
    END_BEFORE_START,
    TOO_LONG,
    NOTE_TOO_LONG,
    TOO_MANY_BREAKS,
    BREAK_END_BEFORE_START,
    BREAK_OUTSIDE_SHIFT,
    BREAKS_OVERLAP,
    OPEN_BREAK_IN_CLOSED_SHIFT,
    MORE_THAN_ONE_OPEN_BREAK,
    OVERLAPS_OTHER_SHIFT,
}

/**
 * Reglas de una jornada, puras y deterministas. Una pausa sólo es válida dentro de su jornada y sin solaparse con
 * otra; dos jornadas no pueden solaparse. El tiempo trabajado se mide siempre con instantes reales, así que cruzar
 * la medianoche o un cambio de hora no es un problema: sólo importa el orden de los instantes.
 */
public object ShiftValidator {
    /**
     * Problemas de [shift] frente a [others] (el resto de jornadas; si incluye la misma jornada por id se ignora) en
     * el instante [now]. Vacío = válida.
     */
    public fun validate(
        shift: WorkShift,
        others: List<WorkShift>,
        now: Instant,
    ): Set<ShiftIssue> {
        val issues = mutableSetOf<ShiftIssue>()
        validateBounds(shift, now, issues)
        if (shift.note.length > WorkRules.SHIFT_NOTE_MAX_LENGTH) issues += ShiftIssue.NOTE_TOO_LONG
        validateBreaks(shift, now, issues)
        val boundsOk = ShiftIssue.END_BEFORE_START !in issues
        if (boundsOk && others.any { it.id != shift.id && overlaps(shift, it, now) }) {
            issues += ShiftIssue.OVERLAPS_OTHER_SHIFT
        }
        return issues
    }

    private fun validateBounds(
        shift: WorkShift,
        now: Instant,
        issues: MutableSet<ShiftIssue>,
    ) {
        if (shift.start > now) issues += ShiftIssue.START_IN_FUTURE
        val end = shift.end ?: return
        when {
            end <= shift.start -> issues += ShiftIssue.END_BEFORE_START
            Duration.between(shift.start, end).seconds > WorkRules.MAX_SHIFT_SECONDS -> issues += ShiftIssue.TOO_LONG
        }
        if (end > now) issues += ShiftIssue.END_IN_FUTURE
    }

    private fun validateBreaks(
        shift: WorkShift,
        now: Instant,
        issues: MutableSet<ShiftIssue>,
    ) {
        if (shift.breaks.size > WorkRules.MAX_BREAKS) issues += ShiftIssue.TOO_MANY_BREAKS
        if (shift.breaks.count { it.isOpen } > 1) issues += ShiftIssue.MORE_THAN_ONE_OPEN_BREAK
        val limit = shift.end ?: now
        shift.breaks.forEach { pause ->
            val end = pause.end
            when {
                end == null && !shift.isOpen -> issues += ShiftIssue.OPEN_BREAK_IN_CLOSED_SHIFT
                end != null && end <= pause.start -> issues += ShiftIssue.BREAK_END_BEFORE_START
            }
            if (pause.start < shift.start || pause.start > limit || (end != null && end > limit)) {
                issues += ShiftIssue.BREAK_OUTSIDE_SHIFT
            }
        }
        // Solapes: ordenadas por inicio, cada pausa debe empezar cuando la anterior ya terminó.
        val ordered = shift.breaks.sortedBy { it.start }
        ordered.zipWithNext().forEach { (previous, next) ->
            val previousEnd = previous.end
            if (previousEnd == null || next.start < previousEnd) issues += ShiftIssue.BREAKS_OVERLAP
        }
    }

    /**
     * Intervalo `[inicio, fin)` que ocupa una jornada. Si está abierta, hasta [now]; y una olvidada (cuyo fin real
     * se desconoce) sólo ocupa el tramo que tardaría en darse por olvidada, para no bloquear días enteros.
     */
    private fun occupiedEnd(
        shift: WorkShift,
        now: Instant,
    ): Instant =
        shift.end
            ?: minOf(now, shift.start.plusSeconds(WorkRules.FORGOTTEN_AFTER_SECONDS))

    private fun overlaps(
        a: WorkShift,
        b: WorkShift,
        now: Instant,
    ): Boolean = a.start < occupiedEnd(b, now) && b.start < occupiedEnd(a, now)
}
