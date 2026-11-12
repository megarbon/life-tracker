@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.quit.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.quit.QuitRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Quit habits. */
@Serializable
public data object QuitDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.quitGraph() {
    composable<QuitDestination> { QuitRoute() }
}
