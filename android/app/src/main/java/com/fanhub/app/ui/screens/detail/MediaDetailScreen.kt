package com.fanhub.app.ui.screens.detail

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fanhub.app.ui.components.FullscreenPlayer
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.ErrorRed
import com.fanhub.app.ui.theme.GoldStar
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.PrimaryPink20
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: Int,
    serverUrl: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {},
    viewModel: MediaDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId) { viewModel.load(mediaId) }
    val uiState by viewModel.uiState.collectAsState()

    // 全屏状态
    var isFullscreen by remember { mutableStateOf(false) }
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current

    // 全屏模式时隐藏系统栏 - 由 FullscreenPlayer 组件内部处理
    // SideEffect 不再需要手动控制系统栏

    DisposableEffect(Unit) {
        onDispose { 
            viewModel.releasePlayer()
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    val selectedTagIds = remember { mutableStateListOf<Int>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(if (isFullscreen) Color.Black else Background)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
            return@Box
        }

        val video = uiState.video ?: return@Box

        if (isFullscreen) {
            // 全屏模式：使用共享组件
            FullscreenPlayer(
                player = viewModel.player,
                onExit = { isFullscreen = false }
            )
        } else {
            // 正常模式
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // 视频播放器区域 - 添加顶部内边距避免太靠上
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = viewModel.player
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    // 全屏按钮
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "全屏", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // 操作按钮栏 - 水平排列在视频下方
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "返回",
                        tint = TextPrimary,
                        onClick = onBack
                    )
                    
                    ActionButton(
                        icon = if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (video.isLiked) "已喜欢" else "喜欢",
                        tint = if (video.isLiked) ErrorRed else TextSecondary,
                        onClick = { viewModel.toggleLike() }
                    )
                    
                    ActionButton(
                        icon = if (video.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        label = if (video.isFavorite) "已收藏" else "收藏",
                        tint = if (video.isFavorite) GoldStar else TextSecondary,
                        onClick = { viewModel.toggleFavorite() }
                    )
                    
                    ActionButton(
                        icon = Icons.Default.Edit,
                        label = "编辑",
                        tint = TextSecondary,
                        onClick = {
                            editedTitle = video.title
                            editedDescription = video.description ?: ""
                            selectedTagIds.clear()
                            selectedTagIds.addAll(video.tags.map { it.id })
                            showEditDialog = true
                        }
                    )
                    
                    ActionButton(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        tint = ErrorRed,
                        onClick = { showDeleteDialog = true }
                    )
                }

                // 视频信息区域
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // 标题
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 描述
                    if (!video.description.isNullOrBlank()) {
                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 标签
                    if (video.tags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(video.tags) { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = PrimaryPink20),
                                    border = null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 元信息
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (video.viewCount > 0) {
                            InfoRow(label = "播放次数", value = "${video.viewCount} 次")
                        }
                        if (video.duration != null && video.duration > 0) {
                            InfoRow(label = "时长", value = video.durationFormatted)
                        }
                        video.resolution?.let { resolution ->
                            InfoRow(label = "分辨率", value = resolution)
                        }
                        if (video.fileSizeFormatted.isNotEmpty()) {
                            InfoRow(label = "文件大小", value = video.fileSizeFormatted)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 删除确认对话框
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("确认删除") },
                    text = { Text("确定要删除 \"${video.title}\" 吗？删除后将无法恢复。") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteVideo()
                                showDeleteDialog = false
                                onDeleted()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("删除")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // 编辑对话框
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("编辑视频信息") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                label = { Text("标题") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { editedDescription = it },
                                label = { Text("描述") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "标签",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            if (uiState.allTags.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(uiState.allTags) { tag ->
                                        FilterChip(
                                            selected = selectedTagIds.contains(tag.id),
                                            onClick = {
                                                if (selectedTagIds.contains(tag.id)) {
                                                    selectedTagIds.remove(tag.id)
                                                } else {
                                                    selectedTagIds.add(tag.id)
                                                }
                                            },
                                            label = { Text(tag.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryPink,
                                                containerColor = BackgroundCard
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateVideoInfo(editedTitle, editedDescription, selectedTagIds.toList())
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                        ) {
                            Text("保存")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(tint.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}