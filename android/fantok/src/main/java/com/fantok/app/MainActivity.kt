package com.fantok.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fantok.app.data.local.SettingsDataStore
import com.fantok.app.player.PlayerManager
import com.fantok.app.ui.navigation.AppNavGraph
import com.fantok.app.ui.navigation.Screen
import com.fantok.app.ui.theme.FantokTheme
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
            FantokTheme {
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