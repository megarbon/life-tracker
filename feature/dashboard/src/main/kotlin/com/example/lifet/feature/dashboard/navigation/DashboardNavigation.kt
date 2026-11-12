@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.dashboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.dashboard.DashboardRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe del dashboard "Hoy". */
@Serializable
public data object DashboardDestination

/** La feature no conoce a otras features: la navegación saliente llegará como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.dashboardGraph(
    onOpenMood: () -> Unit,
    onOpenFinance: () -> Unit,
) {
    composable<DashboardDestination> { DashboardRoute(onOpenMood = onOpenMood, onOpenFinance = onOpenFinance) }
}
