@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Settings. */
@Serializable
public data object SettingsDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.settingsGraph() {
    composable<SettingsDestination> { SettingsRoute() }
}
