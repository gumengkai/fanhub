package com.fantok.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fantok.app.ui.theme.BackgroundBlack
import com.fantok.app.ui.theme.DouyinRed
import com.fantok.app.ui.theme.TextPrimary
import com.fantok.app.ui.theme.TextSecondary
import com.fantok.app.ui.theme.TextTertiary

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf(uiState.urlInput) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = DouyinRed,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "FanTok",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DouyinRed
            )

            Text(
                "极简抖音体验",
                fontSize = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; viewModel.onUrlInputChange(it) },
                label = { Text("服务器地址", color = TextSecondary) },
                placeholder = { Text("http://192.168.x.x:5000", color = TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { scope.launch { viewModel.testConnection() } }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DouyinRed,
                    unfocusedBorderColor = TextTertiary,
                    focusedContainerColor = BackgroundBlack,
                    unfocusedContainerColor = BackgroundBlack,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { scope.launch { viewModel.testConnection() } },
                enabled = !uiState.isTesting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DouyinRed)
            ) {
                if (uiState.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary
                    )
                } else {
                    Text("连接服务器", color = TextPrimary)
                }
            }

            uiState.testResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = result,
                    fontSize = 14.sp,
                    color = if (uiState.testSuccess) DouyinRed else TextTertiary
                )

                if (uiState.testSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.saveUrl()
                                onComplete()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DouyinRed.copy(alpha = 0.8f))
                    ) {
                        Text("开始使用", color = TextPrimary)
                    }
                }
            }
        }
    }
}