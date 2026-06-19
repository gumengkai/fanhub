package com.fanpeak.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fanpeak.app.data.model.Video
import com.fanpeak.app.ui.theme.Background
import com.fanpeak.app.ui.theme.BackgroundCard
import com.fanpeak.app.ui.theme.BorderColor
import com.fanpeak.app.ui.theme.PrimaryRed
import com.fanpeak.app.ui.theme.TextPrimary
import com.fanpeak.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    serverUrl: String,
    onNavigateToDetail: (Int) -> Unit,
    initialTagId: Int? = null,  // 新增：初始标签ID
    viewModel: VideoLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 延迟初始化，等界面准备好后再加载数据
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // 设置初始标签
    LaunchedEffect(initialTagId) {
        if (initialTagId != null && initialTagId != uiState.selectedTagId) {
            viewModel.selectTag(initialTagId)
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearError()
            }
        }
    }

    // Infinite scroll - 添加防抖和状态检查
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // 确保有更多数据且不在加载中
            lastVisible >= totalItems - 6 && uiState.hasMore && !uiState.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadVideos()
        }
    }

    // 排序下拉框状态
    var sortExpanded by remember { mutableStateOf(false) }
    // 筛选下拉框状态
    var filterExpanded by remember { mutableStateOf(false) }
    // 删除确认对话框
    var videoToDelete by remember { mutableStateOf<Video?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // 性能优化：限制预加载和缓存
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        // 标题栏
        item(span = { GridItemSpan(2) }) {
            TopAppBar(
                title = {
                    Text(
                        "视频库",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }

        // 搜索框
        item(span = { GridItemSpan(2) }) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                placeholder = { Text("搜索视频...", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "搜索", tint = TextSecondary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // 控制栏：排序 + 筛选
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 排序按钮
                Box {
                    FilterChip(
                        selected = false,
                        onClick = { sortExpanded = true },
                        label = { Text(uiState.sortOption.label) },
                        leadingIcon = {
                            Icon(Icons.Default.Sort, contentDescription = "排序", modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = BackgroundCard
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = BorderColor
                        )
                    )
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setSort(option)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }

                // 筛选按钮
                Box {
                    FilterChip(
                        selected = uiState.filterType != FilterType.ALL,
                        onClick = { filterExpanded = true },
                        label = {
                            Text(
                                when (uiState.filterType) {
                                    FilterType.ALL -> "全部"
                                    FilterType.LIKED -> "已喜欢"
                                    FilterType.FAVORITE -> "已收藏"
                                    FilterType.UNWATCHED -> "未观看"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                when (uiState.filterType) {
                                    FilterType.LIKED -> Icons.Default.Favorite
                                    FilterType.FAVORITE -> Icons.Default.Star
                                    FilterType.UNWATCHED -> Icons.Default.VisibilityOff
                                    else -> Icons.Default.FilterList
                                },
                                contentDescription = "筛选",
                                modifier = Modifier.size(16.dp),
                                tint = when (uiState.filterType) {
                                    FilterType.LIKED -> Color(0xFFFF4D4F)
                                    FilterType.FAVORITE -> Color(0xFFFAAD14)
                                    else -> TextSecondary
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed,
                            containerColor = BackgroundCard
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.filterType != FilterType.ALL,
                            selectedBorderColor = PrimaryRed,
                            borderColor = BorderColor
                        )
                    )
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部视频") },
                            onClick = {
                                viewModel.setFilter(FilterType.ALL)
                                filterExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFFF4D4F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("已喜欢")
                                }
                            },
                            onClick = {
                                viewModel.setFilter(FilterType.LIKED)
                                filterExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFAAD14),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("已收藏")
                                }
                            },
                            onClick = {
                                viewModel.setFilter(FilterType.FAVORITE)
                                filterExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("未观看")
                                }
                            },
                            onClick = {
                                viewModel.setFilter(FilterType.UNWATCHED)
                                filterExpanded = false
                            }
                        )
                    }
                }

                // 显示总数
                Text(
                    "共 ${uiState.total} 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }
        }

        // 标签筛选行
        if (uiState.tags.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTagId == null,
                            onClick = { viewModel.selectTag(null) },
                            label = { Text("全部标签") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed,
                                containerColor = BackgroundCard
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedTagId == null,
                                selectedBorderColor = PrimaryRed,
                                borderColor = BorderColor
                            )
                        )
                    }
                    items(uiState.tags) { tag ->
                        FilterChip(
                            selected = uiState.selectedTagId == tag.id,
                            onClick = { viewModel.selectTag(tag.id) },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed,
                                containerColor = BackgroundCard
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedTagId == tag.id,
                                selectedBorderColor = PrimaryRed,
                                borderColor = BorderColor
                            )
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            }
        } else if (uiState.videos.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("暂无视频", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                }
            }
        } else {
            items(
                items = uiState.videos,
                key = { "${it.id}_${it.hashCode()}" }
            ) { video ->
                VideoGridCard(
                    video = video,
                    serverUrl = serverUrl,
                    onClick = { onNavigateToDetail(video.id) },
                    onFavorite = { viewModel.toggleFavorite(video) },
                    onLike = { viewModel.toggleLike(video) },
                    onDelete = { videoToDelete = video },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (uiState.isLoadingMore) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryRed)
                    }
                }
            }
        }
    }

    // Snackbar
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(bottom = 60.dp)
    )

    // 删除确认对话框
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除 \"${video.title ?: "该视频"}\" 吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun VideoGridCard(
    video: Video,
    serverUrl: String,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 缩略图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClick() }
            ) {
                AsyncImage(
                    model = "${serverUrl}/api/douyin/${video.id}/thumbnail",
                    contentDescription = video.title ?: "视频缩略图",
                    contentScale = ContentScale.Crop,
                    // 限制图片尺寸以减少内存占用
                    modifier = Modifier.fillMaxSize(),
                    onError = { /* 静默处理加载错误 */ }
                )

                // 时长标签
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // 喜欢/收藏/删除按钮
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "喜欢",
                            tint = if (video.isLiked) Color(0xFFFF4D4F) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (video.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "收藏",
                            tint = if (video.isFavorite) Color(0xFFFAAD14) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 标题
            Text(
                text = video.title ?: "未命名视频",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )

            // 信息行
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.fileSizeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (video.viewCount > 0) {
                    Text(
                        text = "• ${video.viewCount}次播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}