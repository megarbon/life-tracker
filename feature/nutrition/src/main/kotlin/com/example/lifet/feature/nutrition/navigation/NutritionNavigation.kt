@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.nutrition.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.nutrition.NutritionRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Food. */
@Serializable
public data object NutritionDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.nutritionGraph() {
    composable<NutritionDestination> { NutritionRoute() }
}
