@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.insights.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.insights.InsightsRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Trends. */
@Serializable
public data object InsightsDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.insightsGraph() {
    composable<InsightsDestination> { InsightsRoute() }
}
