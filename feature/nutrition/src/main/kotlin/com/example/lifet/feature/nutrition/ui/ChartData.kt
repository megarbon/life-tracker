package com.example.lifet.feature.nutrition.ui

import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.model.nutrition.NutritionSeries
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val ROLLING_WINDOW_DAYS = 7

/** kcal por día (lun, mar…). Los días sin registrar valen 0: el titular y el resumen aclaran cuántos hay. */
internal fun NutritionSeries.dailyKcalPoints(locale: Locale): List<ChartPoint> =
    days.map { day ->
        ChartPoint(label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), value = day.nutrients.kcal)
    }

/**
 * Tendencia: media móvil de 7 días con etiqueta "día/mes". Los días cuya ventana no tiene datos se omiten, así un
 * hueco largo sin registrar no dibuja ceros falsos.
 */
internal fun NutritionSeries.trendPoints(locale: Locale): List<ChartPoint> {
    val formatter = DateTimeFormatter.ofPattern("d/M", locale)
    return rollingAverageKcal(ROLLING_WINDOW_DAYS).map { (date, kcal) -> ChartPoint(date.format(formatter), kcal) }
}
