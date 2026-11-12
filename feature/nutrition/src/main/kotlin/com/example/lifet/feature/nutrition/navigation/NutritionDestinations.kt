package com.example.lifet.feature.nutrition.navigation

import kotlinx.serialization.Serializable

/*
 * Destinos internos del área. La ruta pública para la app es `NutritionDestination`; dentro, Alimentación tiene su
 * propio NavHost, así la app no cambia cuando se añaden pantallas. Los argumentos son primitivos para que los
 * ViewModels los lean del `SavedStateHandle` sin depender de la navegación.
 */

@Serializable
internal data object DiaryDestination

@Serializable
internal data class SearchDestination(
    val mealType: String,
    val epochDay: Long,
)

@Serializable
internal data class FoodDetailDestination(
    val foodId: String,
    val mealType: String,
    val epochDay: Long,
)

@Serializable
internal data class CustomFoodDestination(
    val foodId: String? = null,
)

@Serializable
internal data object GoalsDestination

@Serializable
internal data object TrendsDestination

/** Claves de los argumentos de arriba, que coinciden con los nombres de las propiedades. */
internal object NavArgs {
    const val MEAL_TYPE = "mealType"
    const val EPOCH_DAY = "epochDay"
    const val FOOD_ID = "foodId"

    /** Resultado que el detalle deja para el Diario: ids de lo añadido, separados por coma. */
    const val ADDED_IDS = "nutrition_added_ids"
}
