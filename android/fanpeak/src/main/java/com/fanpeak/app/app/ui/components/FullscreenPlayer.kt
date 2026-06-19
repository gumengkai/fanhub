package com.fanpeak.app.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * 全屏播放器组件 - 可被短视频页面和视频详情页共享使用
 * 
 * 功能：
 * - 隐藏系统状态栏和导航栏
 * - 竖向全屏显示
 * - 完整播放控件：进度条、播放速度、音量、快进快退
 * - 退出时恢复系统栏和屏幕方向
 */
@Composable
fun FullscreenPlayer(
    player: ExoPlayer,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val systemUiController = rememberSystemUiController()

    // 播放状态
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isDraggingProgress by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    // Hide system bars for fullscreen
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
        systemUiController.isStatusBarVisible = false
        systemUiController.isNavigationBarVisible = false
        activity?.let { act ->
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            WindowCompat.setDecorFitsSystemWindows(act.window, false)
        }
    }

    // Update progress frequently
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(200)
            val dur = player.duration
            val pos = player.currentPosition
            if (dur > 0) {
                duration = (dur / 1000).toInt()
                currentPosition = (pos / 1000).toInt()
                if (!isDraggingProgress) {
                    seekPosition = (pos.toFloat() / dur).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Auto hide controls after 5s
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(5000)
            showControls = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            systemUiController.isStatusBarVisible = true
            systemUiController.isNavigationBarVisible = true
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                WindowCompat.setDecorFitsSystemWindows(act.window, true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // Player view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            onExit()
                            true
                        } else {
                            false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar: Exit button
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.FullscreenExit, "退出全屏", tint = Color.White)
                }

                // Center: Big play button + skip buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { player.seekTo(player.currentPosition - 10000) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, "快退10秒", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // Play/Pause
                    IconButton(
                        onClick = { player.playWhenReady = !player.playWhenReady },
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            if (player.playWhenReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (player.playWhenReady) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { player.seekTo(player.currentPosition + 10000) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FastForward, "快进10秒", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    // Progress slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatTime(currentPosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        Slider(
                            value = if (isDraggingProgress) seekPosition else (player.currentPosition.toFloat() / player.duration.coerceAtLeast(1)),
                            onValueChange = { newValue ->
                                isDraggingProgress = true
                                seekPosition = newValue
                            },
                            onValueChangeFinished = {
                                isDraggingProgress = false
                                val seekTo = (seekPosition * player.duration).toLong()
                                player.seekTo(seekTo)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE53935),
                                activeTrackColor = Color(0xFFE53935),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            formatTime(duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Secondary controls: Volume + Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                player.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                if (isMuted) "取消静音" else "静音",
                                tint = if (isMuted) Color(0xFFE53935) else Color.White
                            )
                        }

                        Spacer(Modifier.width(24.dp))

                        // Speed control
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                                    val currentIdx = speeds.indexOf(playbackSpeed)
                                    playbackSpeed = speeds[(currentIdx + 1) % speeds.size]
                                    player.setPlaybackSpeed(playbackSpeed)
                                }
                        ) {
                            Text(
                                "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (playbackSpeed != 1f) Color(0xFFE53935) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}:${secs.toString().padStart(2, '0')}"
}

private fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}