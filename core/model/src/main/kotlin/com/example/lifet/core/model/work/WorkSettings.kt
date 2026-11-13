package com.example.lifet.core.model.work

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Ajustes de Trabajo: horas objetivo a la semana y qué días se trabaja. El objetivo diario es el semanal repartido
 * a partes iguales entre los días laborables; los demás días no tienen objetivo (lo que se trabaje en ellos es saldo
 * a favor).
 */
public data class WorkSettings(
    public val weeklyTargetMinutes: Int = WorkRules.DEFAULT_WEEKLY_TARGET_MINUTES,
    public val workingDays: Set<DayOfWeek> = DEFAULT_WORKING_DAYS,
) {
    public val weeklyTargetSeconds: Long get() = weeklyTargetMinutes * WorkRules.SECONDS_PER_MINUTE

    /** Objetivo de [date] en segundos; cero si no es un día laborable. */
    public fun targetSecondsOn(date: LocalDate): Long =
        if (date.dayOfWeek in workingDays && workingDays.isNotEmpty()) {
            weeklyTargetSeconds / workingDays.size
        } else {
            0L
        }

    /** Un objetivo semanal razonable y al menos un día laborable. */
    public val isValid: Boolean
        get() = weeklyTargetMinutes in 1..WorkRules.MAX_WEEKLY_TARGET_MINUTES && workingDays.isNotEmpty()

    public companion object {
        public val DEFAULT_WORKING_DAYS: Set<DayOfWeek> =
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            )
    }
}

/** Nota libre de un día ("Reunión con el cliente, pendiente enviar presupuesto"). Una por día. */
public data class WorkDayNote(
    public val date: LocalDate,
    public val text: String,
)

/** Todo el contenido de Trabajo. El repositorio lo emite entero; la calculadora lo convierte en resúmenes. */
public data class WorkDataSet(
    public val shifts: List<WorkShift> = emptyList(),
    public val projects: List<WorkProject> = emptyList(),
    public val focusSessions: List<FocusSession> = emptyList(),
    public val notes: List<WorkDayNote> = emptyList(),
    public val settings: WorkSettings = WorkSettings(),
) {
    /** `true` si el área no se ha usado nunca (proyectos y ajustes solos no cuentan como uso). */
    public val hasNoRecords: Boolean get() = shifts.isEmpty() && focusSessions.isEmpty() && notes.isEmpty()
}
