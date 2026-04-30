package com.fantok.app.ui.screens.feed

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fantok.app.data.model.Video
import com.fantok.app.ui.theme.BackgroundBlack
import com.fantok.app.ui.theme.DouyinGold
import com.fantok.app.ui.theme.DouyinRed
import com.fantok.app.ui.theme.TextSecondary
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun FeedScreen(
    initialFilterType: String = "all",
    startVideoId: Int? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val systemUiController = rememberSystemUiController()

    // 使用key来确保每次参数变化时重新初始化
    LaunchedEffect(initialFilterType, startVideoId) {
        viewModel.resetAndInitialize(initialFilterType, startVideoId)
    }

    SideEffect {
        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
        systemUiController.isStatusBarVisible = false
        systemUiController.isNavigationBarVisible = false
    }

    var showLikeAnimation by remember { mutableStateOf(false) }
    var likeAnimationAlpha by remember { mutableStateOf(0f) }
    val animatedAlpha by animateFloatAsState(targetValue = likeAnimationAlpha)

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.playlist.size }
    )

    LaunchedEffect(uiState.currentIndex) {
        if (pagerState.settledPage != uiState.currentIndex && uiState.currentIndex < uiState.playlist.size) {
            pagerState.scrollToPage(uiState.currentIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageSettled(page)
        }
    }

    LaunchedEffect(uiState.showControls, uiState.playlist) {
        if (uiState.showControls && uiState.playlist.isNotEmpty()) {
            delay(3000)
            viewModel.hideControls()
        }
    }

    DisposableEffect(uiState.playlist) {
        if (uiState.playlist.isEmpty()) {
            return@DisposableEffect onDispose { }
        }

        val player = viewModel.playerManager.player
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    duration = player.duration
                }
            }
        }
        player.addListener(listener)

        val progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(500)
                if (!isSeeking && player.isPlaying) {
                    currentPosition = player.currentPosition
                }
            }
        }

        onDispose {
            player.removeListener(listener)
            progressJob.cancel()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = DouyinRed,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.error!!,
                    color = TextSecondary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = DouyinRed)
                ) {
                    Text("重试", color = Color.White)
                }
            }
        } else if (uiState.playlist.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (uiState.filterType) {
                        "liked" -> "暂无喜欢的视频"
                        "favorite" -> "暂无收藏的视频"
                        else -> "暂无视频"
                    },
                    color = TextSecondary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.setFilterType("all") },
                    colors = ButtonDefaults.buttonColors(containerColor = DouyinRed)
                ) {
                    Text("查看全部", color = Color.White)
                }
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = uiState.playlist[page]
                val isActive = pagerState.settledPage == page

                VideoFeedItem(
                    video = video,
                    isActive = isActive,
                    player = viewModel.playerManager.player,
                    serverUrl = viewModel.playerManager.getServerUrl(),
                    onDoubleTap = {
                        if (!video.isLiked) viewModel.toggleLike(video.id)
                        showLikeAnimation = true
                        likeAnimationAlpha = 1f
                    },
                    onSingleTap = { viewModel.toggleControlsVisibility() }
                )
            }

            if (showLikeAnimation) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = DouyinRed,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp)
                        .alpha(animatedAlpha)
                        .scale(animatedAlpha * 1.2f)
                )
                LaunchedEffect(showLikeAnimation) {
                    delay(800)
                    likeAnimationAlpha = 0f
                    showLikeAnimation = false
                }
            }

            // 顶部控制栏 - 左上方下拉筛选
            if (uiState.showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 筛选下拉按钮
                        FilterDropdown(
                            currentFilter = uiState.filterType,
                            onFilterSelected = { viewModel.setFilterType(it) }
                        )

                        // 随机/顺序切换
                        IconButton(
                            onClick = { viewModel.toggleRandomMode() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = if (uiState.isRandom) "随机" else "顺序",
                                tint = if (uiState.isRandom) DouyinRed else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 计数器 - 显示当前索引/当前播放列表总数（筛选后）以及总视频数
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(top = 48.dp)
                        ) {
                            // 当前播放列表计数
                            Text(
                                text = "${uiState.currentIndex + 1} / ${uiState.playlist.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            // 总视频数（后台同步中显示总数）
                            if (uiState.totalCount > 0) {
                                Text(
                                    text = "共${uiState.totalCount}个视频",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            // 同步中提示
                            if (uiState.isSyncing) {
                                Text(
                                    text = "同步中...",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 右侧操作按钮 - 竖向排列（抖音经典布局）
            val currentVideo = uiState.playlist.getOrNull(uiState.currentIndex)
            if (currentVideo != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // 喜欢
                    ActionButtonVertical(
                        icon = if (currentVideo.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = "喜欢",
                        color = if (currentVideo.isLiked) DouyinRed else Color.White,
                        onClick = { viewModel.toggleLike(currentVideo.id) }
                    )
                    // 收藏
                    ActionButtonVertical(
                        icon = if (currentVideo.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        label = "收藏",
                        color = if (currentVideo.isFavorite) DouyinGold else Color.White,
                        onClick = { viewModel.toggleFavorite(currentVideo.id) }
                    )
                    // 删除
                    ActionButtonVertical(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        color = Color.White.copy(alpha = 0.8f),
                        onClick = {
                            videoToDelete = currentVideo
                            showDeleteDialog = true
                        }
                    )
                }

                // 底部信息区域
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 100.dp, bottom = 80.dp)
                ) {
                    // 视频标题
                    Text(
                        text = currentVideo.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2
                    )
                }
            }

            // 进度条
            if (uiState.showControls && duration > 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 60.dp)
                        .fillMaxWidth()
                ) {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { newValue ->
                            isSeeking = true
                            currentPosition = newValue.toLong()
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            viewModel.playerManager.player.seekTo(currentPosition)
                        },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showDeleteDialog && videoToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; videoToDelete = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除 \"${videoToDelete?.title}\" 吗？删除后将无法恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            videoToDelete?.let { viewModel.deleteVideo(it.id) }
                            showDeleteDialog = false
                            videoToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DouyinRed)
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false; videoToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) DouyinRed.copy(alpha = 0.9f)
                else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = Color.White
        )
    }
}

@Composable
private fun ActionButtonVertical(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = color.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun FilterDropdown(
    currentFilter: String,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filterText = when (currentFilter) {
        "liked" -> "喜欢"
        "favorite" -> "收藏"
        else -> "全部"
    }

    Box {
        Row(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = filterText,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "筛选",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
        ) {
            DropdownMenuItem(
                text = { Text("全部视频", color = Color.White) },
                onClick = {
                    onFilterSelected("all")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("喜欢的视频", color = Color.White) },
                onClick = {
                    onFilterSelected("liked")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("收藏的视频", color = Color.White) },
                onClick = {
                    onFilterSelected("favorite")
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun VideoFeedItem(
    video: Video,
    isActive: Boolean,
    player: ExoPlayer,
    serverUrl: String,
    onDoubleTap: () -> Unit,
    onSingleTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onTap = { onSingleTap() }
                )
            }
    ) {
        if (isActive && serverUrl.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
