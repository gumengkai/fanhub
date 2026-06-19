package com.fanpeak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fanpeak.app.data.local.SettingsDataStore
import com.fanpeak.app.player.PlayerManager
import com.fanpeak.app.ui.navigation.AppNavGraph
import com.fanpeak.app.ui.navigation.Screen
import com.fanpeak.app.ui.theme.FanPeakTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(playerManager)

        setContent {
            FanPeakTheme {
                val serverUrl by settingsDataStore.serverUrl.collectAsState(initial = "")
                val startDestination = if (serverUrl.isBlank()) {
                    Screen.Onboarding.route
                } else {
                    Screen.Home.route
                }
                AppNavGraph(
                    startDestination = startDestination,
                    settingsDataStore = settingsDataStore
                )
            }
        }
    }
}