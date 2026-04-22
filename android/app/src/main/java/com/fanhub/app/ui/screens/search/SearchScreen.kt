package com.fanhub.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fanhub.app.ui.components.VideoCard
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    serverUrl: String,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("搜索视频…", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = SearchBarDefaults.colors(containerColor = BackgroundCard),
            content = {}
        )

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
            uiState.query.isBlank() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("输入关键词开始搜索", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            uiState.results.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到相关内容", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "找到 ${uiState.results.size} 个结果",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(uiState.results, key = { it.id }) { video ->
                        VideoCard(video = video, serverUrl = serverUrl, onClick = { onNavigateToDetail(video.id) })
                    }
                }
            }
        }
    }
}