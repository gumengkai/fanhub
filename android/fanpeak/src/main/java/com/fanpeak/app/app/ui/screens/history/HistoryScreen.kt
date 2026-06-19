package com.fanpeak.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fanpeak.app.ui.theme.Background
import com.fanpeak.app.ui.theme.BackgroundCard
import com.fanpeak.app.ui.theme.BorderColor
import com.fanpeak.app.ui.theme.PrimaryRed
import com.fanpeak.app.ui.theme.TextPrimary
import com.fanpeak.app.ui.theme.TextSecondary
import com.fanpeak.app.ui.theme.TextTertiary

data class HistoryItem(
    val id: Int,
    val videoId: Int,
    val videoTitle: String,
    val playbackPosition: Int,
    val isCompleted: Boolean,
    val watchedAt: String?,
    val videoDuration: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    serverUrl: String,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 延迟加载数据
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TopAppBar(
            title = {
                Text(
                    "播放历史",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            actions = {
                if (uiState.items.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearAll) {
                        Icon(Icons.Default.Delete, contentDescription = "清空历史", tint = TextSecondary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed)
            }
        } else {
            LazyColumn {
                uiState.grouped.forEach { (group, items) ->
                    item(key = "header_$group") {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(items, key = { it.id }) { item ->
                        val progress = if (item.videoDuration != null && item.videoDuration > 0) {
                            item.playbackPosition.toFloat() / item.videoDuration.toFloat()
                        } else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(item.videoId) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BackgroundCard)
                            ) {
                                AsyncImage(
                                    model = "$serverUrl/api/douyin/${item.videoId}/thumbnail",
                                    contentDescription = item.videoTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(2.dp),
                                    color = PrimaryRed,
                                    trackColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.videoTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "进度 ${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteItem(item.videoId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = TextTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}