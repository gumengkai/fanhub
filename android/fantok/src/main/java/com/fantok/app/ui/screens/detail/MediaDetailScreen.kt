package com.fantok.app.ui.screens.detail

import android.view.ViewGroup
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.fantok.app.ui.theme.Background
import com.fantok.app.ui.theme.ErrorRed
import com.fantok.app.ui.theme.GoldStar
import com.fantok.app.ui.theme.PrimaryPink
import com.fantok.app.ui.theme.TextPrimary
import com.fantok.app.ui.theme.TextSecondary
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

    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current

    // 全屏模式
    DisposableEffect(Unit) {
        systemUiController.isStatusBarVisible = false
        systemUiController.isNavigationBarVisible = false
        onDispose { 
            viewModel.releasePlayer()
            systemUiController.isStatusBarVisible = true
            systemUiController.isNavigationBarVisible = true
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
            return@Box
        }

        val video = uiState.video ?: return@Box

        // 全屏视频播放器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = viewModel.player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "返回",
                tint = Color.White,
                onClick = onBack
            )
            
            ActionButton(
                icon = if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (video.isLiked) "已喜欢" else "喜欢",
                tint = if (video.isLiked) ErrorRed else Color.White,
                onClick = { viewModel.toggleLike() }
            )
            
            ActionButton(
                icon = if (video.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (video.isFavorite) "已收藏" else "收藏",
                tint = if (video.isFavorite) GoldStar else Color.White,
                onClick = { viewModel.toggleFavorite() }
            )
            
            ActionButton(
                icon = Icons.Default.Edit,
                label = "编辑",
                tint = Color.White,
                onClick = {
                    editedTitle = video.title
                    editedDescription = video.description ?: ""
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

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除 \"${video.title}\" 吗？") },
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

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("编辑视频信息") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("标题") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { editedDescription = it },
                            label = { Text("描述") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateVideoInfo(editedTitle, editedDescription)
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

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
