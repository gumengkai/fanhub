package com.fantok.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Feed       : Screen("feed")       // 短视频流
    object Profile    : Screen("profile")    // 个人中心
    object Settings   : Screen("settings")   // 设置
}

// 底部导航：2个 tab（短视频、我）
sealed class BottomTab(val route: String, val label: String) {
    object Feed   : BottomTab(Screen.Feed.route, "首页")
    object Profile: BottomTab(Screen.Profile.route, "我")
}