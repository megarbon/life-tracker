package com.example.lifet.core.model.quit

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Qué suele disparar un antojo. */
public enum class CravingTrigger {
    STRESS,
    EMOTIONS,
    SOCIAL,
    BOREDOM,
    AFTER_MEAL,
    COFFEE,
    WORK_BREAK,
    TIREDNESS,
    ROUTINE,
    OTHER,
}

/** Qué se hizo (o se probó) para atravesar el antojo. */
public enum class CopingStrategy {
    BREATHING,
    WALK,
    WATER,
    CALL_SOMEONE,
    DISTRACTION,
    EXERCISE,
    WAIT,
    SNACK,
    OTHER,
}

/** Algo que ocurrió en un instante concreto (UTC) con la zona del usuario entonces; [date] es su día lógico. */
public sealed interface QuitEvent {
    public val id: String
    public val habitId: String
    public val at: Instant
    public val zone: ZoneId
    public val note: String

    public val date: LocalDate get() = at.atZone(zone).toLocalDate()
}

/** Un antojo: [intensity] va de 1 a 5. */
public data class QuitCraving(
    override val id: String,
    override val habitId: String,
    override val at: Instant,
    override val zone: ZoneId,
    public val intensity: Int,
    public val trigger: CravingTrigger,
    public val strategy: CopingStrategy? = null,
    override val note: String = "",
) : QuitEvent

/**
 * Un consumo (o recaída), sin culpa: no borra el progreso, sólo reinicia la racha actual. [units] son las
 * unidades consumidas (cigarrillos, copas...) y [trigger] lo que lo provocó, si se quiere contar.
 */
public data class QuitConsumption(
    override val id: String,
    override val habitId: String,
    override val at: Instant,
    override val zone: ZoneId,
    public val units: Int,
    public val trigger: CravingTrigger? = null,
    override val note: String = "",
) : QuitEvent
