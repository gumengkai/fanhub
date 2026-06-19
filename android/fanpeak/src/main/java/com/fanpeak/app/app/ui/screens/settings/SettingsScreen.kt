package com.fanpeak.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fanpeak.app.ui.theme.Background
import com.fanpeak.app.ui.theme.BackgroundCard
import com.fanpeak.app.ui.theme.BorderColor
import com.fanpeak.app.ui.theme.ErrorRed
import com.fanpeak.app.ui.theme.PrimaryRed
import com.fanpeak.app.ui.theme.TextPrimary
import com.fanpeak.app.ui.theme.TextSecondary
import com.fanpeak.app.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val autoPlay by viewModel.autoPlay.collectAsState()
    val loopVideo by viewModel.loopVideo.collectAsState()
    val rememberProgress by viewModel.rememberProgress.collectAsState()

    var urlInput by remember(serverUrl) { mutableStateOf(uiState.urlInput.ifBlank { serverUrl }) }

    Column(modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        TopAppBar(
            title = { Text("设置", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        // Server config section
        SectionHeader("服务器配置")
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    viewModel.onUrlInputChange(it)
                },
                label = { Text("服务器地址") },
                placeholder = { Text("http://192.168.1.x:3001", color = TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.testConnection() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryRed,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = BackgroundCard,
                    unfocusedContainerColor = BackgroundCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = viewModel::testConnection,
                    enabled = !uiState.isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                    } else {
                        Text("测试连接")
                    }
                }
                uiState.testResult?.let { result ->
                    Text(
                        text = result,
                        color = if (uiState.testSuccess) PrimaryRed else ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = BorderColor)

        // Playback settings
        SectionHeader("播放设置")
        SettingSwitch("Feed 自动播放", autoPlay, viewModel::setAutoPlay)
        SettingSwitch("循环播放", loopVideo, viewModel::setLoopVideo)
        SettingSwitch("记住播放进度", rememberProgress, viewModel::setRememberProgress)

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = BorderColor)

        // About
        SectionHeader("关于")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("版本", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(
            checked = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = PrimaryRed)
        )
    }
}
