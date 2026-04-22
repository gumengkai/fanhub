package com.fanhub.app.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fanhub.app.data.model.Video
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.SecondaryOrange
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverUrl: String,
    onNavigateToFeed: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "FanHub",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            brush = Brush.linearGradient(listOf(PrimaryPink, SecondaryOrange))
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }

        // 错误提示
        if (uiState.errorMessage != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E4)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ ${uiState.errorMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF4D4F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.loadData() },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "重试", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重试")
                            }
                            Button(
                                onClick = onNavigateToSettings,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("修改地址")
                            }
                        }
                    }
                }
            }
        }

        // Hero pager
        if (uiState.featured.isNotEmpty()) {
            item {
                HeroPager(
                    items = uiState.featured,
                    serverUrl = serverUrl,
                    onItemClick = onNavigateToDetail
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Quick entry grid
        item {
            SectionTitle("快捷入口")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickEntry("视频库", Icons.Default.GridView, Modifier.weight(1f), onNavigateToLibrary)
                QuickEntry("个人中心", Icons.Default.Person, Modifier.weight(1f), onNavigateToProfile)
                QuickEntry("历史", Icons.Default.History, Modifier.weight(1f), onNavigateToHistory)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 我的收藏
        if (uiState.myFavorites.isNotEmpty()) {
            item {
                SectionTitle("我的收藏")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.myFavorites) { video ->
                        SmallMediaCard(
                            title = video.title,
                            thumbnailUrl = "${serverUrl}/api/videos/${video.id}/thumbnail",
                            onClick = { onNavigateToDetail(video.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 今日推荐
        if (uiState.todayRecommend.isNotEmpty()) {
            item {
                SectionTitle("今日推荐", showRefresh = true, onRefresh = { viewModel.loadData() })
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.todayRecommend) { video ->
                        SmallMediaCard(
                            title = video.title,
                            thumbnailUrl = "${serverUrl}/api/videos/${video.id}/thumbnail",
                            onClick = { onNavigateToDetail(video.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (uiState.isLoading && uiState.featured.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPink)
                }
            }
        }

        // 底部留白，避免被导航栏遮挡
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HeroPager(items: List<Video>, serverUrl: String, onItemClick: (Int) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }
    Box {
        HorizontalPager(state = pagerState) { page ->
            val video = items[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { onItemClick(video.id) }
            ) {
                AsyncImage(
                    model = "${serverUrl}/api/videos/${video.id}/thumbnail",
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))
                        )
                )
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                )
            }
        }
        // Dot indicators
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(items.size) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == idx) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == idx) PrimaryPink else Color.White.copy(0.5f))
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, showRefresh: Boolean = false, onRefresh: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        if (showRefresh) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新推荐", tint = PrimaryPink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QuickEntry(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = PrimaryPink, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun HistoryCard(title: String, progress: Float, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundCard)
        ) {
            // Progress bar at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(PrimaryPink)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun SmallMediaCard(title: String, thumbnailUrl: String, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundCard)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}