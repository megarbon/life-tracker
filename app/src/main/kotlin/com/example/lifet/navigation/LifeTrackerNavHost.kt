package com.example.lifet.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lifet.R
import com.example.lifet.feature.dashboard.navigation.DashboardDestination
import com.example.lifet.feature.dashboard.navigation.dashboardGraph
import com.example.lifet.feature.finance.navigation.FinanceDestination
import com.example.lifet.feature.finance.navigation.financeGraph
import com.example.lifet.feature.fitness.navigation.FitnessDestination
import com.example.lifet.feature.fitness.navigation.fitnessGraph
import com.example.lifet.feature.insights.navigation.InsightsDestination
import com.example.lifet.feature.insights.navigation.insightsGraph
import com.example.lifet.feature.mood.navigation.MoodDestination
import com.example.lifet.feature.mood.navigation.moodGraph
import com.example.lifet.feature.nutrition.navigation.NutritionDestination
import com.example.lifet.feature.nutrition.navigation.nutritionGraph
import com.example.lifet.feature.quit.navigation.QuitDestination
import com.example.lifet.feature.quit.navigation.quitGraph
import com.example.lifet.feature.settings.navigation.SettingsDestination
import com.example.lifet.feature.settings.navigation.settingsGraph
import com.example.lifet.feature.work.navigation.WorkDestination
import com.example.lifet.feature.work.navigation.workGraph
import kotlin.reflect.KClass

private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @param:StringRes val label: Int,
) {
    TODAY(DashboardDestination, DashboardDestination::class, Icons.Rounded.Home, R.string.tab_today),
    NUTRITION(NutritionDestination, NutritionDestination::class, Icons.Rounded.Favorite, R.string.tab_nutrition),
    FITNESS(FitnessDestination, FitnessDestination::class, Icons.Rounded.Star, R.string.tab_fitness),
    FINANCE(FinanceDestination, FinanceDestination::class, Icons.Rounded.ShoppingCart, R.string.tab_finance),
    MORE(MoreDestination, MoreDestination::class, Icons.Rounded.Menu, R.string.tab_more),
}

private val moreEntries =
    listOf(
        MoreEntry(R.string.more_mood, MoodDestination),
        MoreEntry(R.string.more_quit, QuitDestination),
        MoreEntry(R.string.more_work, WorkDestination),
        MoreEntry(R.string.more_insights, InsightsDestination),
        MoreEntry(R.string.more_settings, SettingsDestination),
    )

/**
 * Único sitio que conoce todas las features. Cada feature expone su `xGraph()`; así
 * `feature → feature` nunca ocurre y la navegación entre áreas se cablea aquí con lambdas.
 */
@Composable
fun LifeTrackerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab =
        TopLevelTab.entries.firstOrNull { tab ->
            currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
        }

    // Las pantallas de detalle (p. ej. Ánimo desde "Más") ocupan toda la pantalla sin barra.
    val layoutType =
        if (currentTab == null) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
        }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        navigationSuiteItems = {
            TopLevelTab.entries.forEach { tab ->
                item(
                    selected = tab == currentTab,
                    onClick = { navController.navigateToTab(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = DashboardDestination,
        ) {
            dashboardGraph()
            nutritionGraph()
            fitnessGraph()
            financeGraph()
            moodGraph()
            quitGraph()
            workGraph()
            insightsGraph()
            settingsGraph()
            composable<MoreDestination> {
                MoreScreen(
                    entries = moreEntries,
                    onEntryClick = { navController.navigate(it.destination) },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
