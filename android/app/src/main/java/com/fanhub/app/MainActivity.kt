package com.fanhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fanhub.app.data.local.SettingsDataStore
import com.fanhub.app.player.PlayerManager
import com.fanhub.app.ui.navigation.AppNavGraph
import com.fanhub.app.ui.navigation.Screen
import com.fanhub.app.ui.theme.FanHubTheme
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
            FanHubTheme {
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