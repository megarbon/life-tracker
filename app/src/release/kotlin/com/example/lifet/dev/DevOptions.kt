package com.example.lifet.dev

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.example.lifet.navigation.MoreEntry

/** En release no hay opciones de desarrollador. */
internal val devMoreEntries: List<MoreEntry> = emptyList()

@Suppress("UnusedParameter") // Misma firma que la variante debug.
internal fun NavGraphBuilder.devOptionsGraph(navController: NavController) = Unit
