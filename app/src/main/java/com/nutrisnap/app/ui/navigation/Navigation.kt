package com.nutrisnap.app.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nutrisnap.app.ui.NutritionViewModel
import com.nutrisnap.app.ui.screens.HomeScreen
import com.nutrisnap.app.ui.screens.HistoryScreen
import com.nutrisnap.app.ui.screens.AddFoodScreen
import com.nutrisnap.app.ui.screens.AnalyticsScreen
import com.nutrisnap.app.ui.screens.SettingsScreen
import com.nutrisnap.app.ui.screens.ManualFoodScreen
import com.nutrisnap.app.ui.screens.SavedFoodsScreen
import com.nutrisnap.app.ui.screens.GeminiReviewScreen
import com.nutrisnap.app.data.models.MealType
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.safeDrawing

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object AddFood : Screen("add_food")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
    data object ManualFood : Screen("manual_food")
    data object SavedFoods : Screen("saved_foods")
    data object PhotoReview : Screen("photo_review/{photoUri}") {
        fun createRoute(photoUri: String) = "photo_review/${Uri.encode(photoUri)}"
    }
}

private data class NavigationDestination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    NavigationDestination(Screen.Home.route, "Home", Icons.Filled.Home),
    NavigationDestination(Screen.History.route, "History", Icons.Filled.CalendarMonth),
    NavigationDestination(Screen.AddFood.route, "Add", Icons.Filled.Add),
    NavigationDestination(Screen.Analytics.route, "Trends", Icons.Filled.Timeline),
    NavigationDestination(Screen.Settings.route, "Settings", Icons.Filled.Settings)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val viewModel: NutritionViewModel = viewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets.safeDrawing) {
                destinations.forEach { destination ->
                    val selected = when (destination.route) {
                        Screen.AddFood.route -> currentRoute in setOf(
                            Screen.AddFood.route,
                            Screen.ManualFood.route,
                            Screen.SavedFoods.route,
                            Screen.PhotoReview.route
                        )
                        else -> currentRoute == destination.route
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(destination.icon, destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel, onAddFood = { navController.navigate(Screen.AddFood.route) })
            }
            composable(Screen.History.route) { HistoryScreen(viewModel = viewModel) }
            composable(Screen.AddFood.route) {
                AddFoodScreen(
                    onManual = { navController.navigate(Screen.ManualFood.route) },
                    onSavedFoods = { navController.navigate(Screen.SavedFoods.route) },
                    onPhotoSelected = { uri -> navController.navigate(Screen.PhotoReview.createRoute(uri.toString())) }
                )
            }
            composable(Screen.ManualFood.route) {
                ManualFoodScreen(viewModel = viewModel, onSaved = { navController.popBackStack() })
            }
            composable(Screen.SavedFoods.route) {
                SavedFoodsScreen(viewModel = viewModel, onAdded = { navController.popBackStack(Screen.Home.route, false) })
            }
            composable(Screen.Analytics.route) { AnalyticsScreen(viewModel = viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel = viewModel) }
            composable(
                route = Screen.PhotoReview.route,
                arguments = listOf(navArgument("photoUri") { type = NavType.StringType })
            ) { entry ->
                GeminiReviewScreen(
                    photoUri = Uri.decode(entry.arguments?.getString("photoUri").orEmpty()),
                    viewModel = viewModel,
                    onManualEntry = { navController.navigate(Screen.ManualFood.route) },
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack(Screen.Home.route, false) }
                )
            }
        }
    }
}
