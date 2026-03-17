package com.leteam.locked.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.leteam.locked.auth.SignInScreen
import com.leteam.locked.ui.screens.camera.CameraScreen
import com.leteam.locked.ui.screens.camera.PostingScreen
import com.leteam.locked.ui.screens.home.HomeScreen
import com.leteam.locked.ui.screens.profile.ProfileScreen
import com.leteam.locked.ui.screens.profile.EditProfileScreen
import com.leteam.locked.ui.screens.settings.SettingsScreen
import com.leteam.locked.ui.screens.insights.InsightsScreen
import com.leteam.locked.ui.screens.workouts.MyWorkoutsScreen
import com.leteam.locked.ui.screens.search.SearchScreen
import com.leteam.locked.ui.screens.workouts.WorkoutsFeedScreen
import com.leteam.locked.ui.screens.settings.PasswordResetScreen
import com.leteam.locked.ui.screens.home.PostDetailScreen

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScreen(onSignedOut: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Routes.HOME,
        Routes.WORKOUTS,
        Routes.SEARCH,
        Routes.PROFILE,
        Routes.SETTINGS
    )

    val items = listOf(
        NavItem(Routes.HOME, "Home", Icons.Default.Home),
        NavItem(Routes.WORKOUTS, "Workouts", Icons.Default.FitnessCenter),
        NavItem(Routes.SEARCH, "Search", Icons.Default.Search),
        NavItem(Routes.PROFILE, "Profile", Icons.Default.Person),
        NavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        items.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    indicatorColor = Color.White,
                                    unselectedIconColor = Color.DarkGray,
                                    unselectedTextColor = Color.DarkGray
                                )
                            )
                        }
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
                    onPostClick = { navController.navigate(Routes.CAMERA) },
                    onUserClick = { userId -> navController.navigate(Routes.profileUser(userId)) },
                    onHeaderProfileClick = {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Routes.WORKOUTS) {
                WorkoutsFeedScreen(
                    onWorkoutOpen = { navController.navigate(Routes.MYWORKOUTS) },
                    onInsightsClick = { navController.navigate(Routes.INSIGHTS) },
                    onUserClick = { userId -> navController.navigate(Routes.profileUser(userId)) }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(onUserClick = { userId ->
                    navController.navigate(Routes.profileUser(userId))
                })
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onUserClick = { uid -> navController.navigate(Routes.profileUser(uid)) },
                    onPostClick = { postId -> navController.navigate(Routes.postDetail(postId)) },
                    onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) }
                )
            }

            composable(
                route = Routes.PROFILE_USER,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                ProfileScreen(
                    profileUserId = userId,
                    onUserClick = { uid -> navController.navigate(Routes.profileUser(uid)) },
                    onPostClick = { postId -> navController.navigate(Routes.postDetail(postId)) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.POST_DETAIL,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(Routes.profileUser(userId)) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onSignedOut = onSignedOut,
                    onResetPasswordClick= { navController.navigate(Routes.PASSRESET) }
                )
            }
            composable(Routes.PASSRESET){
                PasswordResetScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onBackClick = { navController.popBackStack() },
                    onPostClick = { uri ->
                        navController.navigate(Routes.posting(uri))
                    }
                )
            }
            composable(Routes.MYWORKOUTS) {
                MyWorkoutsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.INSIGHTS) {
                InsightsScreen(
                    onBackClick = { navController.popBackStack() }
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