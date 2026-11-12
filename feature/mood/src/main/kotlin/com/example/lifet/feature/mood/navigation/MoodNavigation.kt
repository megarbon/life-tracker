@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package com.example.lifet.feature.mood.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.feature.mood.MoodRoute
import kotlinx.serialization.Serializable

/** Ruta type-safe de Mood. */
@Serializable
public data object MoodDestination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas `onNavigateToX`. */
public fun NavGraphBuilder.moodGraph() {
    composable<MoodDestination> { MoodRoute() }
}
