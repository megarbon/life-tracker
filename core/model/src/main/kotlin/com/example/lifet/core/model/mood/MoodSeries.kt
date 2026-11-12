package com.example.lifet.core.model.mood

import com.example.lifet.core.model.DateRange
import java.time.LocalDate

/** Un día de la serie. [average] es `null` si no hubo check-ins; [movingAverage] se calcula con los 7 días previos. */
public data class MoodDayPoint(
    public val date: LocalDate,
    public val average: Double?,
    public val movingAverage: Double?,
    public val entryCount: Int,
)

/** Cuántas veces aparece una [emotion] (clave de [MoodTags] o etiqueta propia). */
public data class EmotionCount(
    public val emotion: String,
    public val count: Int,
)

/** Ánimo medio en los días con y sin un [factor]; [delta] positivo = los días con el factor van mejor. */
public data class FactorImpact(
    public val factor: String,
    public val averageWith: Double,
    public val averageWithout: Double,
    public val daysWith: Int,
    public val daysWithout: Int,
) {
    public val delta: Double get() = averageWith - averageWithout
}

/**
 * Serie de ánimo de un [range], pensada para gráficas e Insights.
 *
 * @property average media de las medias diarias del rango o `null` si no hay datos.
 * @property recentDelta diferencia entre los últimos 7 días y los 7 anteriores (si ambos tienen datos).
 */
public data class MoodSeries(
    public val range: DateRange,
    public val days: List<MoodDayPoint>,
    public val average: Double?,
    public val entryCount: Int,
    public val recentDelta: Double?,
    public val topEmotions: List<EmotionCount>,
    public val factorImpacts: List<FactorImpact>,
)
