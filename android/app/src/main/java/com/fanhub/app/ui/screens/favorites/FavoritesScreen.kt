package com.fanhub.app.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fanhub.app.data.model.Favorite
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.SecondaryOrange
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToDetail: (Int) -> Unit = {},
    onNavigateToFavoriteDetail: (Int) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // 抖音风格标题栏
        TopAppBar(
            title = {
                Text(
                    "收藏",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        // 收藏夹网格（抖音风格）
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPink)
                    }
                }
            } else if (uiState.favorites.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无收藏", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(uiState.favorites, key = { it.id }) { fav ->
                    FavoriteCard(
                        favorite = fav,
                        onClick = { onNavigateToFavoriteDetail(fav.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: Favorite,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 封面区域（渐变背景）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryPink.copy(alpha = 0.3f), SecondaryOrange.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = PrimaryPink.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // 信息区域
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    favorite.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${favorite.totalCount} 个内容",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}