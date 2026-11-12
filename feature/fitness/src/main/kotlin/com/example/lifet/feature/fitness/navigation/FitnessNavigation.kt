@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.fitness.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.fitness.FitnessRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Fitness. */
@Serializable
public data object FitnessDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.fitnessGraph() {
    composable<FitnessDestination> { FitnessRoute() }
}
