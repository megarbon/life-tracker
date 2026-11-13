package com.example.lifet.feature.quit

import com.example.lifet.core.model.quit.QuitRules
import java.time.Instant

private const val DAYS_PER_YEAR = 365L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTE_MS = 60_000L
private const val SECOND_MS = 1_000L
private const val MIN_TICK_MS = 50L

/**
 * Un tiempo limpio desglosado para mostrarlo. Los años son de 365 días, igual que los hitos; el resto de unidades son
 * lo que sobra de la mayor. [totalDays] es el número de días completos, que es lo que cuentan los textos.
 */
internal data class CleanTime(
    val years: Long,
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val totalDays: Long get() = years * DAYS_PER_YEAR + days

    /** Sólo el primer día interesa ver los segundos correr; después bastan los minutos. */
    val showsSeconds: Boolean get() = totalDays == 0L

    /** Cada cuánto cambia lo que se ve: cada segundo durante el primer día y cada minuto el resto del tiempo. */
    val tickMillis: Long get() = if (showsSeconds) SECOND_MS else MINUTE_MS

    companion object {
        fun of(totalSeconds: Long): CleanTime {
            val safe = totalSeconds.coerceAtLeast(0L)
            val allDays = safe / QuitRules.SECONDS_PER_DAY
            val rest = safe % QuitRules.SECONDS_PER_DAY
            return CleanTime(
                years = allDays / DAYS_PER_YEAR,
                days = allDays % DAYS_PER_YEAR,
                hours = (rest / SECONDS_PER_HOUR).toInt(),
                minutes = ((rest % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt(),
                seconds = (rest % SECONDS_PER_MINUTE).toInt(),
            )
        }

        /** Tiempo limpio de una racha que empezó en [since], visto desde [now]. Nunca negativo. */
        fun between(
            since: Instant,
            now: Instant,
        ): CleanTime = of(now.epochSecond - since.epochSecond)
    }
}

/**
 * Milisegundos hasta el próximo cambio visible de un contador que empezó en [since]. Se alinea con el segundo o el
 * minuto de la racha (no con el reloj de pared) para que los dígitos cambien justo cuando toca y no se acumule desfase.
 */
internal fun millisUntilNextTick(
    since: Instant,
    now: Instant,
): Long {
    val elapsed = (now.toEpochMilli() - since.toEpochMilli()).coerceAtLeast(0L)
    val tick = CleanTime.of(elapsed / SECOND_MS).tickMillis
    return (tick - elapsed % tick).coerceAtLeast(MIN_TICK_MS)
}
