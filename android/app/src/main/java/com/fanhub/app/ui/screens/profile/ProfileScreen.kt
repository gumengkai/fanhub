package com.fanhub.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.ErrorRed
import com.fanhub.app.ui.theme.GoldStar
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.PrimaryPink20
import com.fanhub.app.ui.theme.SecondaryOrange
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibraryWithTag: (Int) -> Unit = {},  // 新增：跳转到视频库并筛选标签
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 喜欢, 1: 收藏, 2: 标签

    // 根据选择加载对应数据
    val currentVideos = if (selectedTab == 0) uiState.likedVideos else uiState.favoriteVideos

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // 抖音风格顶部栏
        TopAppBar(
            title = {
                Text(
                    "我",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        // 用户信息卡片（抖音风格）
        ProfileHeader(
            likedCount = uiState.likedVideoCount + uiState.likedImageCount,
            favoriteCount = uiState.favoriteVideoCount + uiState.favoriteImageCount,
            tagCount = uiState.tags.size
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 主 Tab：喜欢 / 收藏 / 标签
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Background,
            indicator = { tabPositions ->
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = when (selectedTab) {
                        0 -> ErrorRed
                        1 -> GoldStar
                        else -> PrimaryPink
                    },
                    height = 3.dp
                )
            }
        ) {
            ProfileTab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = Icons.Default.Favorite,
                label = "喜欢",
                count = uiState.likedVideoCount + uiState.likedImageCount,
                selectedColor = ErrorRed
            )
            ProfileTab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = Icons.Default.Star,
                label = "收藏",
                count = uiState.favoriteVideoCount + uiState.favoriteImageCount,
                selectedColor = GoldStar
            )
            ProfileTab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = Icons.Default.Tag,
                label = "标签",
                count = uiState.tags.size,
                selectedColor = PrimaryPink
            )
        }

        // 内容区域
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
        } else if (selectedTab == 2) {
            // 标签页：展示所有标签
            if (uiState.tags.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无标签",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        "点击标签查看相关视频",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 使用 FlowRow 展示标签
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.tags.forEach { tag ->
                            TagChip(
                                tag = tag,
                                onClick = { onNavigateToLibraryWithTag(tag.id) }
                            )
                        }
                    }
                }
            }
        } else {
            // 喜欢/收藏页：展示视频网格
            if (currentVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (selectedTab == 0) Icons.Default.Favorite else Icons.Default.Star,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selectedTab == 0) "暂无喜欢的内容" else "暂无收藏的内容",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentVideos, key = { it.id }) { item ->
                        MediaGridItem(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    likedCount: Int,
    favoriteCount: Int,
    tagCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(PrimaryPink.copy(alpha = 0.1f), SecondaryOrange.copy(alpha = 0.05f))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像（占位）
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "FanHub 用户",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 统计数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(count = likedCount, label = "喜欢", color = ErrorRed)
            StatItem(count = favoriteCount, label = "收藏", color = GoldStar)
            StatItem(count = tagCount, label = "标签", color = PrimaryPink)
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ProfileTab(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    count: Int,
    selectedColor: Color
) {
    Tab(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) selectedColor else TextSecondary
            )
        },
        text = {
            Text(
                "$label $count",
                color = if (selected) selectedColor else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium
            )
        }
    )
}

@Composable
private fun TagChip(
    tag: com.fanhub.app.data.model.Tag,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tag.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tag.videoCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "(${tag.videoCount})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        },
        leadingIcon = {
            Icon(
                Icons.Default.Tag,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PrimaryPink
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PrimaryPink20
        ),
        border = null,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun MediaGridItem(
    item: ProfileMediaItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            
            // 播放图标（视频类型）
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}