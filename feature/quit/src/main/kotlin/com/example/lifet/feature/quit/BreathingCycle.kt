package com.example.lifet.feature.quit

private const val INHALE_SECONDS = 4
private const val HOLD_SECONDS = 4
private const val EXHALE_SECONDS = 6

/** Fases de la respiración guiada: inspirar, mantener y soltar el aire despacio. */
internal enum class BreathPhase(
    val seconds: Int,
) {
    INHALE(INHALE_SECONDS),
    HOLD(HOLD_SECONDS),
    EXHALE(EXHALE_SECONDS),
}

/** Dónde está la respiración: la [phase], los [secondsLeft] de esa fase y los ciclos ya completados. */
internal data class BreathState(
    val phase: BreathPhase,
    val secondsLeft: Int,
    val completedCycles: Int,
)

/** Ciclo 4-4-6, suave y sin retención larga para que cualquiera pueda seguirlo. Puro y determinista. */
internal object BreathingCycle {
    val CYCLE_SECONDS: Int = BreathPhase.entries.sumOf { it.seconds }

    /** Estado tras [elapsedSeconds] segundos de ejercicio (0 es el inicio de la primera inspiración). */
    fun stateAt(elapsedSeconds: Int): BreathState {
        val safe = elapsedSeconds.coerceAtLeast(0)
        var inCycle = safe % CYCLE_SECONDS
        var current = BreathPhase.entries.last()
        for (phase in BreathPhase.entries) {
            if (inCycle < phase.seconds) {
                current = phase
                break
            }
            inCycle -= phase.seconds
        }
        return BreathState(
            phase = current,
            secondsLeft = current.seconds - inCycle,
            completedCycles = safe / CYCLE_SECONDS,
        )
    }
}
