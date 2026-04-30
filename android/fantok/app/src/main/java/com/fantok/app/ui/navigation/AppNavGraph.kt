package com.fantok.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fantok.app.ui.screens.feed.FeedScreen
import com.fantok.app.ui.screens.profile.ProfileScreen
import com.fantok.app.ui.theme.BackgroundBlack
import com.fantok.app.ui.theme.DouyinRed
import com.fantok.app.ui.theme.TextTertiary

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("feed?filterType=all", "首页", Icons.Default.Home),
    BottomNavItem(Screen.Profile.route, "我", Icons.Default.Person)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        containerColor = BackgroundBlack,
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundBlack.copy(alpha = 0.9f),
                tonalElevation = 0.dp,
                modifier = Modifier.height(48.dp)
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (currentRoute == item.route) DouyinRed else TextTertiary
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontSize = 12.sp,
                                color = if (currentRoute == item.route) DouyinRed else TextTertiary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DouyinRed,
                            selectedTextColor = DouyinRed,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = DouyinRed.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "feed?filterType=all",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = "feed?filterType={filterType}&startVideoId={startVideoId}",
                arguments = listOf(
                    navArgument("filterType") {
                        type = NavType.StringType
                        defaultValue = "all"
                    },
                    navArgument("startVideoId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val filterType = backStackEntry.arguments?.getString("filterType") ?: "all"
                val startVideoId = backStackEntry.arguments?.getInt("startVideoId")?.takeIf { it > 0 }
                FeedScreen(initialFilterType = filterType, startVideoId = startVideoId)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToVideo = { videoId, filterType ->
                        navController.navigate("feed?filterType=$filterType&startVideoId=$videoId") {
                            popUpTo("feed?filterType=all") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
