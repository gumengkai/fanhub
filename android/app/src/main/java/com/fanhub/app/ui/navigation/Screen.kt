package com.fanhub.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding     : Screen("onboarding")
    object Home           : Screen("home")           // 首页推荐
    object Feed           : Screen("feed")           // 短视频流
    object Library        : Screen("library")        // 视频库
    object Favorites      : Screen("favorites")      // 收藏列表（二级页面）
    object Profile        : Screen("profile")        // 个人中心
    object History        : Screen("history")
    object Search         : Screen("search")
    object Settings       : Screen("settings")

    object MediaDetail : Screen("detail/{mediaId}") {
        fun createRoute(id: Int) = "detail/$id"
    }
    object FavoriteDetail : Screen("favorite/{favId}") {
        fun createRoute(id: Int) = "favorite/$id"
    }
}

// 底部导航：4个tab（首页、短视频、视频库、我）
sealed class BottomTab(val route: String, val label: String, val iconRes: Int? = null) {
    object Home      : BottomTab(Screen.Home.route,      "首页")
    object Feed      : BottomTab(Screen.Feed.route,      "短视频")
    object Library   : BottomTab(Screen.Library.route,   "视频库")
    object Profile   : BottomTab(Screen.Profile.route,   "我")
}