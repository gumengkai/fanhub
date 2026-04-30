package com.fantok.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiService: ApiService,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val serverUrl = settingsDataStore.serverUrl

    suspend fun testConnection(url: String) {
        _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)

        try {
            settingsDataStore.saveServerUrl(url.trim())
            val response = apiService.healthCheck()
            if (response.isSuccessful && response.body()?.get("status") == "ok") {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "✓ 已连接",
                    testSuccess = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "连接失败",
                    testSuccess = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = "连接失败",
                testSuccess = false
            )
        }
    }

    suspend fun saveUrl(url: String) {
        settingsDataStore.saveServerUrl(url.trim())
    }
}