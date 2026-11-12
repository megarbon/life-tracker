@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.work.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.work.WorkRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Work. */
@Serializable
public data object WorkDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.workGraph() {
    composable<WorkDestination> { WorkRoute() }
}
