package com.example.lifet.core.model.quit

import com.example.lifet.core.model.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Qué se deja. */
public enum class HabitKind { TOBACCO, ALCOHOL }

/** [QUIT] deja el hábito del todo; [REDUCE] lo baja hasta un límite semanal (sólo tiene sentido con alcohol). */
public enum class HabitMode { QUIT, REDUCE }

/** Periodo al que se refiere el consumo previo: "20 al día" o "14 a la semana". */
public enum class BaselinePeriod(
    public val seconds: Long,
) {
    DAY(QuitRules.SECONDS_PER_DAY),
    WEEK(QuitRules.SECONDS_PER_WEEK),
}

/**
 * Un hábito que se quiere dejar o reducir.
 *
 * [startedAt] es el instante en que empieza el contador (UTC) y [zone] la zona del usuario entonces, de modo que su
 * día lógico no cambia si viaja. El consumo previo es [baselineUnits] por [baselinePeriod] (cigarrillos, copas...)
 * y una unidad cuesta `packPrice / unitsPerPack`: con alcohol, `unitsPerPack` es 1 y [packPrice] el precio de una
 * consumición. [weeklyLimit] es el máximo de unidades por semana en modo reducción, y `null` si no aplica.
 */
public data class QuitHabit(
    public val id: String,
    public val kind: HabitKind,
    public val name: String,
    public val mode: HabitMode,
    public val startedAt: Instant,
    public val zone: ZoneId,
    public val baselineUnits: Int,
    public val baselinePeriod: BaselinePeriod,
    public val unitsPerPack: Int,
    public val packPrice: Money,
    public val weeklyLimit: Int? = null,
) {
    /** Día lógico en el que empezó. */
    public val startDate: LocalDate get() = startedAt.atZone(zone).toLocalDate()
}
