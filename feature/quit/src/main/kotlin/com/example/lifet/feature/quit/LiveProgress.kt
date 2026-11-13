package com.example.lifet.feature.quit

import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.HabitProgress
import com.example.lifet.core.model.quit.MilestoneKey
import com.example.lifet.core.model.quit.savedAt
import java.time.Instant

/** Lo que falta para el siguiente hito con la racha en el instante de la consulta. */
internal data class NextMilestone(
    val key: MilestoneKey,
    val remainingSeconds: Long,
    val progress: Float,
)

/**
 * Lo que cambia mientras la pantalla está abierta: el tiempo limpio, el dinero ahorrado y el siguiente hito. Se
 * calcula con el instante [now] a partir del [HabitProgress] (que sólo se recalcula cuando cambian los datos), de
 * modo que el contador avanza sin volver a consultar la base de datos.
 */
internal data class LiveProgress(
    val cleanSeconds: Long,
    val clean: CleanTime,
    val saved: Money,
    val next: NextMilestone?,
)

internal fun liveProgress(
    progress: HabitProgress,
    now: Instant,
): LiveProgress {
    val seconds = (now.epochSecond - progress.cleanSince.epochSecond).coerceAtLeast(0L)
    val keys = MilestoneKey.entries
    val nextIndex = keys.indexOfFirst { it.seconds > seconds }
    val next =
        if (progress.milestones.isEmpty() || nextIndex < 0) {
            null
        } else {
            val previous = keys.getOrNull(nextIndex - 1)?.seconds ?: 0L
            val target = keys[nextIndex].seconds
            NextMilestone(
                key = keys[nextIndex],
                remainingSeconds = target - seconds,
                progress = ((seconds - previous).toFloat() / (target - previous)).coerceIn(0f, 1f),
            )
        }
    return LiveProgress(
        cleanSeconds = seconds,
        clean = CleanTime.of(seconds),
        saved = progress.habit.savedAt(now, progress.consumedUnits),
        next = next,
    )
}
