package com.nutrisnap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutrisnap.app.ui.screens.HomeScreen
import com.nutrisnap.app.ui.screens.HistoryScreen
import com.nutrisnap.app.ui.screens.AddFoodScreen
import com.nutrisnap.app.ui.screens.AnalyticsScreen
import com.nutrisnap.app.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object AddFood : Screen("add_food")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.History.route) { HistoryScreen() }
        composable(Screen.AddFood.route) { AddFoodScreen() }
        composable(Screen.Analytics.route) { AnalyticsScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
