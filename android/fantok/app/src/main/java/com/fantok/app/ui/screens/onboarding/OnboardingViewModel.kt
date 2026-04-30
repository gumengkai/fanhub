package com.fantok.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class OnboardingUiState(
    val urlInput: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val apiService: ApiService,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onUrlInputChange(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url)
    }

    suspend fun testConnection() {
        _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)

        try {
            val baseUrl = _uiState.value.urlInput.trim()
            if (baseUrl.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "请输入服务器地址",
                    testSuccess = false
                )
                return
            }

            val response = apiService.healthCheck()
            if (response.isSuccessful && response.body()?.get("status") == "ok") {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "✓ 连接成功",
                    testSuccess = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "连接失败，请检查地址",
                    testSuccess = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = "连接失败: ${e.message}",
                testSuccess = false
            )
        }
    }

    suspend fun saveUrl() {
        settingsDataStore.saveServerUrl(_uiState.value.urlInput.trim())
    }
}