package com.example.lifet.feature.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.detail.FoodDetailCallbacks
import com.example.lifet.feature.nutrition.detail.FoodDetailRoute
import com.example.lifet.feature.nutrition.diary.DiaryNavigation
import com.example.lifet.feature.nutrition.diary.DiaryRoute
import com.example.lifet.feature.nutrition.food.CustomFoodRoute
import com.example.lifet.feature.nutrition.goals.GoalsRoute
import com.example.lifet.feature.nutrition.navigation.CustomFoodDestination
import com.example.lifet.feature.nutrition.navigation.DiaryDestination
import com.example.lifet.feature.nutrition.navigation.FoodDetailDestination
import com.example.lifet.feature.nutrition.navigation.GoalsDestination
import com.example.lifet.feature.nutrition.navigation.NavArgs
import com.example.lifet.feature.nutrition.navigation.SearchDestination
import com.example.lifet.feature.nutrition.navigation.TrendsDestination
import com.example.lifet.feature.nutrition.search.SearchRoute
import com.example.lifet.feature.nutrition.trends.TrendsRoute

private const val ID_SEPARATOR = ","

/**
 * Punto de entrada del área. Tiene su propio NavHost para que la app solo conozca `NutritionDestination`:
 * añadir pantallas aquí no obliga a tocar la navegación raíz.
 */
@Composable
internal fun NutritionRoute(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = DiaryDestination, modifier = modifier) {
        composable<DiaryDestination> { entry ->
            val handle = entry.savedStateHandle
            val added by handle.getStateFlow(NavArgs.ADDED_IDS, "").collectAsStateWithLifecycle()
            DiaryRoute(
                navigation =
                    DiaryNavigation(
                        onSearch = { meal, date ->
                            navController.navigate(SearchDestination(meal.name, date.toEpochDay()))
                        },
                        onTrends = { navController.navigate(TrendsDestination) },
                        onGoals = { navController.navigate(GoalsDestination) },
                    ),
                addedIds = added.split(ID_SEPARATOR).filter { it.isNotEmpty() },
                onAddedIdsConsumed = { handle[NavArgs.ADDED_IDS] = "" },
            )
        }
        composable<SearchDestination> { entry ->
            val route = entry.toRoute<SearchDestination>()
            SearchRoute(
                mealType = MealType.fromName(route.mealType) ?: MealType.SNACK,
                onBack = navController::popBackStack,
                onFoodClick = { food ->
                    navController.navigate(FoodDetailDestination(food.id, route.mealType, route.epochDay))
                },
                onCreateFood = { navController.navigate(CustomFoodDestination()) },
            )
        }
        composable<FoodDetailDestination> {
            FoodDetailRoute(
                callbacks =
                    FoodDetailCallbacks(
                        onBack = navController::popBackStack,
                        onAdded = { id -> navController.returnToDiaryWith(id) },
                        onEdit = { foodId -> navController.navigate(CustomFoodDestination(foodId)) },
                    ),
            )
        }
        composable<CustomFoodDestination> { CustomFoodRoute(onBack = navController::popBackStack) }
        composable<GoalsDestination> { GoalsRoute(onBack = navController::popBackStack) }
        composable<TrendsDestination> { TrendsRoute(onBack = navController::popBackStack) }
    }
}

/** Vuelve al Diario (saltando Buscar) y le deja el id añadido para que ofrezca "Deshacer". */
private fun NavHostController.returnToDiaryWith(id: String) {
    val diary =
        try {
            getBackStackEntry(DiaryDestination)
        } catch (_: IllegalArgumentException) {
            null // El Diario es el inicio del grafo; si no está, no hay a quién avisar.
        }
    diary?.savedStateHandle?.set(NavArgs.ADDED_IDS, id)
    popBackStack(DiaryDestination, inclusive = false)
}
