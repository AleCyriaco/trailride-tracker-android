package com.trailride.tracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trailride.tracker.ui.compare.CompareScreen
import com.trailride.tracker.ui.detail.RideDetailScreen
import com.trailride.tracker.ui.history.HistoryScreen
import com.trailride.tracker.ui.liveride.LiveRideScreen
import com.trailride.tracker.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Ride : Screen("ride", "Ride", Icons.Default.DirectionsBike)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Compare : Screen("compare", "Compare", Icons.Default.CompareArrows)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val bottomTabs = listOf(Screen.Ride, Screen.History, Screen.Compare, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail screen
    val showBottomBar = currentDestination?.route?.startsWith("ride_detail") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Ride.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Ride.route) {
                LiveRideScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onRideClick = { rideId ->
                        navController.navigate("ride_detail/$rideId")
                    },
                )
            }
            composable(Screen.Compare.route) {
                CompareScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "ride_detail/{rideId}",
                arguments = listOf(navArgument("rideId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val rideId = backStackEntry.arguments?.getLong("rideId") ?: return@composable
                RideDetailScreen(
                    rideId = rideId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
