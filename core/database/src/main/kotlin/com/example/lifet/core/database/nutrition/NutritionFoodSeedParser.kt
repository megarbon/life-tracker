package com.example.lifet.core.database.nutrition

private const val SEPARATOR = ';'
private val REQUIRED_NUMBERS = listOf("kcal", "protein", "carbs", "fat", "serving_g")

/**
 * Convierte el CSV de la semilla (`assets/nutrition/foods_es.csv`, separador `;`, primera línea = cabecera) en
 * entidades. Las columnas se localizan por nombre, así que reordenarlas no rompe nada. Las líneas incompletas o con
 * números inválidos se descartan: un error en una fila no debe impedir cargar el resto.
 */
internal fun parseFoodSeedCsv(csv: String): List<NutritionFoodEntity> {
    val lines =
        csv
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    val header = lines.firstOrNull()?.split(SEPARATOR)?.map { it.trim() } ?: return emptyList()
    val index = header.withIndex().associate { (position, name) -> name to position }
    return lines.drop(1).mapNotNull { line -> SeedRow(line.split(SEPARATOR), index).toEntity() }
}

private class SeedRow(
    private val cells: List<String>,
    private val index: Map<String, Int>,
) {
    fun text(column: String): String? = index[column]?.let(cells::getOrNull)?.trim()?.takeIf { it.isNotEmpty() }

    fun number(column: String): Double? = text(column)?.toDoubleOrNull()?.takeIf { it >= 0.0 }

    fun toEntity(): NutritionFoodEntity? {
        val id = text("id")
        val name = text("name")
        val numbers = REQUIRED_NUMBERS.associateWith { number(it) }
        val serving = numbers["serving_g"] ?: 0.0
        val complete = id != null && name != null && numbers.values.none { it == null }
        return if (complete && serving >= 1.0) build(id.orEmpty(), name.orEmpty(), numbers) else null
    }

    private fun build(
        id: String,
        name: String,
        numbers: Map<String, Double?>,
    ): NutritionFoodEntity =
        NutritionFoodEntity(
            id = id,
            name = name,
            category = text("category") ?: "OTHER",
            kcalPer100g = numbers["kcal"] ?: 0.0,
            proteinPer100g = numbers["protein"] ?: 0.0,
            carbsPer100g = numbers["carbs"] ?: 0.0,
            fatPer100g = numbers["fat"] ?: 0.0,
            fiberPer100g = number("fiber") ?: 0.0,
            sugarPer100g = number("sugar") ?: 0.0,
            servingGrams = (numbers["serving_g"] ?: 0.0).toInt(),
            isCustom = false,
            isFavorite = false,
        )
}
