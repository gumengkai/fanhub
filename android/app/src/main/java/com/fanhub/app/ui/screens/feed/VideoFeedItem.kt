package com.fanhub.app.ui.screens.feed

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.fanhub.app.data.model.Video
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.ErrorRed
import com.fanhub.app.ui.theme.GoldStar
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.PrimaryPink20
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

@Composable
fun VideoFeedItem(
    video: Video,
    isActive: Boolean,
    player: ExoPlayer,
    serverUrl: String,
    isRandom: Boolean = false,
    onLike: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onProgressSync: (Float) -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrev: () -> Unit = {},
    onFullscreen: () -> Unit = {},  // 全屏回调
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var showLikeHeart by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isDraggingProgress by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    // val activity = context as? Activity
    // val systemUiController = rememberSystemUiController()

    // Sync progress every 5s when active
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            delay(5000)
            val dur = player.duration
            val pos = player.currentPosition
            if (dur > 0 && !isDraggingProgress) {
                progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
                onProgressSync(progress)
            }
        }
    }

    // Update progress bar frequently for visual feedback
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            delay(200)
            val dur = player.duration
            val pos = player.currentPosition
            if (dur > 0) {
                duration = (dur / 1000).toInt()
                currentPosition = (pos / 1000).toInt()
                if (!isDraggingProgress) {
                    progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Auto hide controls after 3s
    LaunchedEffect(isActive, showControls) {
        if (!isActive || !showControls) return@LaunchedEffect
        delay(3000)
        showControls = false
    }

    // Auto play next when video ends (随机模式生效)
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            delay(500)
            if (player.duration > 0 && player.currentPosition >= player.duration - 500) {
                onNext()
                break
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                        showLikeHeart = true
                        onLike()
                    }
                )
            }
    ) {
        // Video or thumbnail
        if (isActive) {
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
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val thumbUrl = "${serverUrl.trimEnd('/')}/api/videos/${video.id}/thumbnail"
            AsyncImage(
                model = thumbUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Center play button when paused
        AnimatedVisibility(
            visible = isActive && !player.playWhenReady && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = { player.playWhenReady = true },
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryPink.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 72.dp, bottom = 80.dp, top = 48.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2
                )
                if (!video.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
                if (video.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        video.tags.take(3).forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = PrimaryPink20
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        }

        // Right-side action buttons (抖音风格)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 喜欢
            ActionButton(
                icon = if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "喜欢",
                tint = if (video.isLiked) PrimaryPink else TextPrimary,
                onClick = onLike
            )

            // 收藏
            ActionButton(
                icon = if (video.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                label = "收藏",
                tint = if (video.isFavorite) GoldStar else TextPrimary,
                onClick = onFavorite
            )

            // 全屏
            ActionButton(
                icon = Icons.Default.Fullscreen,
                label = "全屏",
                tint = TextPrimary,
                onClick = onFullscreen
            )

            // 编辑标签
            ActionButton(
                icon = Icons.Default.Edit,
                label = "编辑",
                tint = TextPrimary,
                onClick = onEdit
            )

            // 删除
            ActionButton(
                icon = Icons.Default.Delete,
                label = "删除",
                tint = ErrorRed,
                onClick = onDelete
            )
        }

        // 抖音风格底部进度条 + 倍速控制
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)  // 避开右侧按钮区域
        ) {
            // 拖动时显示时间气泡
            if (isDraggingProgress) {
                val seekTime = (seekPosition * duration).toInt()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        formatTime(seekTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 抖音风格细进度条
                Slider(
                    value = if (isDraggingProgress) seekPosition else progress,
                    onValueChange = { 
                        isDraggingProgress = true
                        seekPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingProgress = false
                        player.seekTo((seekPosition * player.duration).toLong())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),  // 细条
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPink,
                        activeTrackColor = PrimaryPink,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                
                Spacer(Modifier.width(12.dp))
                
                // 倍速控制
                Box(
                    modifier = Modifier
                        .background(
                            if (playbackSpeed != 1f) PrimaryPink.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable {
                            val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                            playbackSpeed = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                            player.setPlaybackSpeed(playbackSpeed)
                        }
                ) {
                    Text(
                        "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (playbackSpeed != 1f) PrimaryPink else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Heart animation overlay (双击动画)
        AnimatedVisibility(
            visible = showLikeHeart,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LaunchedEffect(showLikeHeart) {
                if (showLikeHeart) {
                    delay(800)
                    showLikeHeart = false
                }
            }
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(BackgroundCard.copy(alpha = 0.6f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun formatTime(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}:${secs.toString().padStart(2, '0')}"
}