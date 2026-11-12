package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.Nutrients
import java.io.File

/** Lee el CSV real de la semilla (módulo hermano) para que los menús de demo se validen contra datos reales. */
object SeedCatalog {
    private val file = File("../database/src/main/assets/nutrition/foods_es.csv")

    val foods: Map<String, FoodItem> by lazy {
        check(file.exists()) { "No se encuentra ${file.absolutePath}" }
        file
            .readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line -> line.split(';') }
            .associate { cells ->
                cells[0] to
                    FoodItem(
                        id = cells[0],
                        name = cells[1],
                        category = FoodCategory.fromName(cells[2]),
                        per100g =
                            Nutrients(
                                kcal = cells[3].toDouble(),
                                proteinG = cells[4].toDouble(),
                                carbsG = cells[5].toDouble(),
                                fatG = cells[6].toDouble(),
                                fiberG = cells[7].toDouble(),
                                sugarG = cells[8].toDouble(),
                            ),
                        servingGrams = cells[9].toInt(),
                    )
            }
    }
}
