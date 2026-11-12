@file:Suppress("MatchingDeclarationName") // Rutas + grafo + entradas viven juntos por convención

package com.example.lifet.dev

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.lifet.R
import com.example.lifet.navigation.MoreEntry
import kotlinx.serialization.Serializable

@Serializable
internal data object DevOptionsDestination

@Serializable
internal data object ComponentGalleryDestination

/** Entrada extra en "Más" que sólo existe en builds debug. */
internal val devMoreEntries: List<MoreEntry> = listOf(MoreEntry(R.string.dev_entry, DevOptionsDestination))

internal fun NavGraphBuilder.devOptionsGraph(navController: NavController) {
    composable<DevOptionsDestination> {
        DevOptionsRoute(
            onOpenGallery = { navController.navigate(ComponentGalleryDestination) },
            onBack = navController::popBackStack,
        )
    }
    composable<ComponentGalleryDestination> { ComponentGalleryScreen(onBack = navController::popBackStack) }
}
