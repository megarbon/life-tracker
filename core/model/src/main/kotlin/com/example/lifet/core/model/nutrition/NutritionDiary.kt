package com.example.lifet.core.model.nutrition

/**
 * Lo que necesita la pantalla Diario: el día elegido ([day]) y la ventana de los 7 días que terminan en él
 * ([week]), para dibujar la gráfica semanal sin una segunda suscripción.
 */
public data class NutritionDiary(
    public val day: NutritionDay,
    public val week: NutritionSeries,
)
