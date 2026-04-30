package com.fantok.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fantok.app.player.PlayerManager
import com.fantok.app.ui.navigation.AppNavGraph
import com.fantok.app.ui.theme.FanTokTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerManager: PlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(playerManager)

        setContent {
            FanTokTheme {
                // 直接使用Feed页面，无需设置页
                AppNavGraph()
            }
        }
    }
}
