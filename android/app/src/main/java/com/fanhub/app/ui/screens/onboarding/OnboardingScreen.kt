package com.fanhub.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fanhub.app.ui.theme.Background
import com.fanhub.app.ui.theme.BackgroundCard
import com.fanhub.app.ui.theme.BorderColor
import com.fanhub.app.ui.theme.PrimaryPink
import com.fanhub.app.ui.theme.SecondaryOrange
import com.fanhub.app.ui.theme.TextPrimary
import com.fanhub.app.ui.theme.TextSecondary
import com.fanhub.app.ui.theme.ErrorRed

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.navigateAway) {
        onComplete()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / title
            Text(
                text = "FanHub",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(listOf(PrimaryPink, SecondaryOrange))
                )
            )
            Text(
                text = "连接到你的媒体服务器",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("服务器地址") },
                placeholder = { Text("http://192.168.1.x:3001", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.testAndSave() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPink,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = BackgroundCard,
                    unfocusedContainerColor = BackgroundCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            when {
                uiState.isLoading -> CircularProgressIndicator(color = PrimaryPink)
                uiState.errorMsg != null -> Text(
                    text = uiState.errorMsg!!,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
                uiState.connected -> Text(
                    text = "已连接",
                    color = PrimaryPink,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = viewModel::testAndSave,
                enabled = !uiState.isLoading && uiState.url.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                Text("连接", fontSize = 16.sp, color = TextPrimary)
            }
        }
    }
}
