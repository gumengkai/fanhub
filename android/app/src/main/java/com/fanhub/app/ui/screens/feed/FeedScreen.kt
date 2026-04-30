package com.fanhub.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fanhub.app.data.model.Video
import com.fanhub.app.ui.components.FullscreenPlayer
import com.fanhub.app.ui.theme.ErrorRed
import com.fanhub.app.ui.theme.GoldStar
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.TextSecondary
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val systemUiController = rememberSystemUiController()

    // 延迟加载数据
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // 全屏状态
    var isFullscreen by remember { mutableStateOf(false) }

    // Hide system bars for immersive experience
    SideEffect {
        if (!isFullscreen) {
            systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
            systemUiController.isStatusBarVisible = false
            systemUiController.isNavigationBarVisible = false
        }
    }

    val pagerState = rememberPagerState(pageCount = { uiState.playlist.size })

    // 当 currentIndex 改变时（如切换随机模式），同步 pagerState
    LaunchedEffect(uiState.currentIndex) {
        if (pagerState.settledPage != uiState.currentIndex && uiState.currentIndex < uiState.playlist.size) {
            pagerState.scrollToPage(uiState.currentIndex)
        }
    }

    // Drive player when settled page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageSettled(page)
        }
    }

    // Edit dialog state - 只包含标签选择
    var showEditDialog by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<Video?>(null) }
    val selectedTagIds = remember { mutableStateListOf<Int>() }

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = PrimaryPink,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null && uiState.playlist.isEmpty()) {
            Text(
                text = uiState.error!!,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.playlist.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (uiState.filterType) {
                        "liked" -> "暂无喜欢的视频"
                        "favorite" -> "暂无收藏的视频"
                        "unwatched" -> "暂无未观看的视频"
                        else -> "暂无视频"
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.setFilterType("all") },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) {
                    Text("查看全部")
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
                    isRandom = uiState.isRandom,
                    onLike = { viewModel.toggleLike(video.id) },
                    onFavorite = { viewModel.toggleFavorite(video.id) },
                    onProgressSync = { progress -> viewModel.syncProgress(video.id, progress) },
                    onDelete = {
                        videoToDelete = video
                        showDeleteDialog = true
                    },
                    onEdit = {
                        editingVideo = video
                        selectedTagIds.clear()
                        selectedTagIds.addAll(video.tags.map { it.id })
                        showEditDialog = true
                    },
                    onNext = { viewModel.nextVideo() },
                    onPrev = { viewModel.prevVideo() },
                    onFullscreen = { isFullscreen = true }
                )
            }

            // 顶部控制栏 - 重构筛选
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 第一行：返回 + 计数器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }

                    // 计数器
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${uiState.currentIndex + 1} / ${uiState.playlist.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // 第二行：筛选器（两个下拉）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 筛选1：全部/收藏/喜欢
                    var filterExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = filterExpanded,
                        onExpandedChange = { filterExpanded = it }
                    ) {
                        FilterChip(
                            selected = uiState.filterType != "all",
                            onClick = { filterExpanded = true },
                            label = { 
                                Text(
                                    when (uiState.filterType) {
                                        "liked" -> "喜欢"
                                        "favorite" -> "收藏"
                                        "unwatched" -> "未观看"
                                        else -> "全部"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (uiState.filterType) {
                                        "liked" -> Icons.Default.Favorite
                                        "favorite" -> Icons.Default.Star
                                        "unwatched" -> Icons.Default.VisibilityOff
                                        else -> Icons.Default.VideoLibrary
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = when (uiState.filterType) {
                                        "liked" -> ErrorRed
                                        "favorite" -> GoldStar
                                        "unwatched" -> PrimaryPink
                                        else -> TextSecondary
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPink,
                                containerColor = Color.Black.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = filterExpanded,
                            onDismissRequest = { filterExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部") },
                                onClick = { viewModel.setFilterType("all"); filterExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, "喜欢", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("喜欢")
                                    }
                                },
                                onClick = { viewModel.setFilterType("liked"); filterExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, "收藏", tint = GoldStar, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("收藏")
                                    }
                                },
                                onClick = { viewModel.setFilterType("favorite"); filterExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VisibilityOff, "未观看", tint = PrimaryPink, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("未观看")
                                    }
                                },
                                onClick = { viewModel.setFilterType("unwatched"); filterExpanded = false }
                            )
                        }
                    }

                    // 筛选2：标签下拉
                    if (uiState.tags.isNotEmpty()) {
                        var tagExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = tagExpanded,
                            onExpandedChange = { tagExpanded = it }
                        ) {
                            FilterChip(
                                selected = uiState.selectedTagId != null,
                                onClick = { tagExpanded = true },
                                label = { 
                                    Text(
                                        uiState.tags.find { it.id == uiState.selectedTagId }?.name ?: "标签",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Tag,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPink,
                                    containerColor = Color.Black.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = tagExpanded,
                                onDismissRequest = { tagExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部标签") },
                                    onClick = { viewModel.setSelectedTag(null); tagExpanded = false }
                                )
                                uiState.tags.forEach { tag ->
                                    DropdownMenuItem(
                                        text = { Text(tag.name) },
                                        onClick = { viewModel.setSelectedTag(tag.id); tagExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // 随机播放按钮
                    RandomPlayToggle(
                        isRandom = uiState.isRandom,
                        onToggle = { viewModel.toggleRandomMode() }
                    )
                }
            }
        }
    }

    // 全屏播放器
    if (isFullscreen) {
        FullscreenPlayer(
            player = viewModel.playerManager.player,
            onExit = { isFullscreen = false }
        )
    }

    // Delete confirmation dialog
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
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
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

    // Edit video info dialog - 只包含标签选择
    if (showEditDialog && editingVideo != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false; editingVideo = null },
            title = { Text("编辑标签") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "选择标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    // 标签列表
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.tags.forEach { tag ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedTagIds.contains(tag.id)) {
                                            selectedTagIds.remove(tag.id)
                                        } else {
                                            selectedTagIds.add(tag.id)
                                        }
                                    }
                                    .background(
                                        if (selectedTagIds.contains(tag.id)) PrimaryPink.copy(alpha = 0.2f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = if (selectedTagIds.contains(tag.id)) PrimaryPink else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    tag.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedTagIds.contains(tag.id)) PrimaryPink else TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingVideo?.let { video ->
                            viewModel.updateVideoTags(video.id, selectedTagIds.toList())
                        }
                        showEditDialog = false
                        editingVideo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false; editingVideo = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun RandomPlayToggle(
    isRandom: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (isRandom) PrimaryPink.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.Shuffle,
            contentDescription = if (isRandom) "随机播放" else "顺序播放",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isRandom) "随机" else "顺序",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}