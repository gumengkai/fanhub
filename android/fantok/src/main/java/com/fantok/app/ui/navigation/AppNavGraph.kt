package com.fantok.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fantok.app.data.local.SettingsDataStore
import com.fantok.app.ui.screens.detail.MediaDetailScreen
import com.fantok.app.ui.screens.favorites.FavoriteDetailScreen
import com.fantok.app.ui.screens.favorites.FavoritesScreen
import com.fantok.app.ui.screens.profile.ProfileScreen
import com.fantok.app.ui.screens.feed.FeedScreen
import com.fantok.app.ui.screens.history.HistoryScreen
import com.fantok.app.ui.screens.home.HomeScreen
import com.fantok.app.ui.screens.library.VideoLibraryScreen
import com.fantok.app.ui.screens.onboarding.OnboardingScreen
import com.fantok.app.ui.screens.search.SearchScreen
import com.fantok.app.ui.screens.settings.SettingsScreen

import com.fantok.app.ui.theme.BackgroundSurface
import com.fantok.app.ui.theme.BorderColor
import com.fantok.app.ui.theme.PrimaryPink
import com.fantok.app.ui.theme.TextSecondary
import com.fantok.app.ui.theme.TextTertiary
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

data class BottomNavItem(val tab: String, val label: String, val icon: ImageVector)

// 底部导航：4个tab（首页、短视频、视频库、我）
private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route,      "首页",     Icons.Default.Home),
    BottomNavItem(Screen.Feed.route,      "短视频",   Icons.Default.PlayArrow),
    BottomNavItem(Screen.Library.route,   "视频库",   Icons.Default.VideoLibrary),
    BottomNavItem(Screen.Profile.route,   "我",       Icons.Default.Person),
)

// Routes that hide the bottom bar (full-screen immersive)
private val fullScreenRoutes = setOf(Screen.Feed.route, Screen.Onboarding.route)

@Composable
fun AppNavGraph(
    startDestination: String,
    settingsDataStore: SettingsDataStore
) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomBar = currentRoute !in fullScreenRoutes &&
            !currentRoute.orEmpty().startsWith("detail/")

    // Get server URL for passing to screens
    val serverUrl = runBlocking { settingsDataStore.serverUrl.first() }

    Scaffold(
        containerColor = com.fantok.app.ui.theme.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = BackgroundSurface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.tab,
                            onClick = {
                                navController.navigate(item.tab) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryPink,
                                selectedTextColor = PrimaryPink,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = com.fantok.app.ui.theme.PrimaryPink10
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = if (showBottomBar) Modifier.padding(innerPadding) else Modifier
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Feed.route) {
                FeedScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.MediaDetail.createRoute(id))
                    },
                    onBack = { navController.navigate(Screen.Home.route) }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    serverUrl = serverUrl,
                    onNavigateToFeed = { navController.navigate(Screen.Feed.route) },
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Library.route) {
                VideoLibraryScreen(
                    serverUrl = serverUrl,
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) },
                    onNavigateToFavoriteDetail = { favId -> navController.navigate(Screen.FavoriteDetail.createRoute(favId)) }
                )
            }
            composable(
                route = Screen.FavoriteDetail.route,
                arguments = listOf(navArgument("favId") { type = NavType.IntType })
            ) { back ->
                FavoriteDetailScreen(
                    favId = back.arguments?.getInt("favId") ?: 0,
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    serverUrl = serverUrl,
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    serverUrl = serverUrl,
                    onNavigateToDetail = { id -> navController.navigate(Screen.MediaDetail.createRoute(id)) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.MediaDetail.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.IntType })
            ) { back ->
                MediaDetailScreen(
                    mediaId = back.arguments?.getInt("mediaId") ?: 0,
                    serverUrl = serverUrl,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}