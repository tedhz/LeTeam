package com.leteam.locked.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import com.leteam.locked.ui.screens.camera.CameraScreen
import com.leteam.locked.ui.screens.camera.PostingScreen
import com.leteam.locked.ui.screens.home.HomeScreen
import com.leteam.locked.ui.screens.profile.ProfileScreen
import com.leteam.locked.ui.screens.settings.SettingsScreen

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(
        Routes.HOME,
        Routes.PROFILE,
        Routes.SETTINGS
    )

    val items = listOf(
        NavItem(Routes.HOME, "Home", Icons.Default.Home),
        NavItem(Routes.PROFILE, "Profile", Icons.Default.Person),
        NavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onPostClick = { navController.navigate(Routes.CAMERA) }
                )
            }

            composable(Routes.PROFILE) { ProfileScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onBackClick = { navController.popBackStack() },
                    onPostClick = { uri ->
                        navController.navigate(Routes.posting(uri))
                    }
                )
            }

            composable(
                route = Routes.POSTING,
                arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val imageUriString = backStackEntry.arguments?.getString("imageUri") ?: ""
                val imageUri = Uri.parse(imageUriString)

                PostingScreen(
                    imageUri = imageUri,
                    onPostSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }
    }
}