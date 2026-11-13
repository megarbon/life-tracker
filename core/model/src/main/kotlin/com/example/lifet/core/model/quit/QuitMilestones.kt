@file:Suppress("MagicNumber") // Tabla de hitos: cada duración es un dato, no una regla.

package com.example.lifet.core.model.quit

import java.time.Instant

private const val DAY = QuitRules.SECONDS_PER_DAY
private const val SECONDS_PER_HOUR = 3_600L

/** Hitos de tiempo limpio, de menor a mayor. Un mes son 30 días y un año 365: son referencias, no calendario. */
public enum class MilestoneKey(
    public val seconds: Long,
) {
    HOURS_24(DAY),
    HOURS_48(2 * DAY),
    HOURS_72(3 * DAY),
    WEEK_1(7 * DAY),
    WEEKS_2(14 * DAY),
    MONTH_1(30 * DAY),
    MONTHS_3(90 * DAY),
    MONTHS_6(180 * DAY),
    YEAR_1(365 * DAY),
    YEARS_2(730 * DAY),
    YEARS_5(1_825 * DAY),
}

/**
 * Estado de un hito de un hábito.
 *
 * Un tropiezo no borra lo conseguido: [reachedEver] es verdadero si alguna racha (la actual o una anterior) llegó
 * a él, y no vuelve a ser falso. [reachedNow] y [remainingSeconds] hablan de la racha actual, que es la que
 * sigue creciendo y la que da el siguiente reto.
 *
 * @property reachedAt cuándo se alcanzó (o se alcanzará) en la racha actual, si no hay más consumos.
 * @property progress avance en la racha actual entre el hito anterior y éste, de 0 a 1 (1 si ya lo alcanzó).
 */
public data class Milestone(
    public val key: MilestoneKey,
    public val reachedAt: Instant,
    public val reachedNow: Boolean,
    public val reachedEver: Boolean,
    public val remainingSeconds: Long,
    public val progress: Float,
) {
    /** `true` si la racha actual lo alcanzó hace menos de [QuitRules.NEW_MILESTONE_HOURS] horas respecto a [now]. */
    public fun isRecent(now: Instant): Boolean =
        reachedNow && now.epochSecond - reachedAt.epochSecond < QuitRules.NEW_MILESTONE_HOURS * SECONDS_PER_HOUR
}

/** Hitos para una racha de [cleanSeconds] que empezó en [cleanSince] y un récord de [bestStreakSeconds]. */
internal fun milestonesFor(
    cleanSince: Instant,
    cleanSeconds: Long,
    bestStreakSeconds: Long,
): List<Milestone> =
    MilestoneKey.entries.mapIndexed { index, key ->
        val previous = MilestoneKey.entries.getOrNull(index - 1)?.seconds ?: 0L
        val reachedNow = cleanSeconds >= key.seconds
        Milestone(
            key = key,
            reachedAt = cleanSince.plusSeconds(key.seconds),
            reachedNow = reachedNow,
            reachedEver = reachedNow || bestStreakSeconds >= key.seconds,
            remainingSeconds = (key.seconds - cleanSeconds).coerceAtLeast(0L),
            progress =
                if (reachedNow) {
                    1f
                } else {
                    ((cleanSeconds - previous).toFloat() / (key.seconds - previous)).coerceIn(0f, 1f)
                },
        )
    }
