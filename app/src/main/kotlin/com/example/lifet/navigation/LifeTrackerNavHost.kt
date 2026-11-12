package com.example.lifet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.lifet.feature.dashboard.navigation.DashboardDestination
import com.example.lifet.feature.dashboard.navigation.dashboardGraph

/**
 * Único sitio que conoce todas las features. Cada feature expone su `xGraph()`; así
 * `feature → feature` nunca ocurre y la navegación entre áreas se cablea aquí con lambdas.
 */
@Composable
fun LifeTrackerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = DashboardDestination,
        modifier = modifier,
    ) {
        dashboardGraph()
    }
}
