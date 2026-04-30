package com.fanhub.app.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BorderColor
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.PrimaryPink20
import com.fanhub.app.ui.theme.SecondaryOrange
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagBrowserScreen(
    onTagClick: (String) -> Unit,
    viewModel: TagBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 延迟加载数据
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "标签浏览",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPink)
                }
            }
        } else {
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.tags.forEachIndexed { idx, tag ->
                        // Scale font 12sp~22sp based on relative position
                        val fontSize = (12 + (idx % 5) * 2.5f).sp
                        val isHighFreq = idx < uiState.tags.size / 3
                        SuggestionChip(
                            onClick = { onTagClick(tag.name) },
                            label = {
                                Text(
                                    tag.name,
                                    fontSize = fontSize,
                                    color = if (isHighFreq) PrimaryPink else TextSecondary
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isHighFreq) PrimaryPink20 else Background
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isHighFreq) PrimaryPink else BorderColor
                            )
                        )
                    }
                }
            }
        }
    }
}
