@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.finance.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.finance.FinanceRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Finance. */
@Serializable
public data object FinanceDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.financeGraph() {
    composable<FinanceDestination> { FinanceRoute() }
}
