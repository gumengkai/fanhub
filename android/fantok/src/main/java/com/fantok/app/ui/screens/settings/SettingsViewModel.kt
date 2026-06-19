package com.fantok.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.api.BaseUrlInterceptor
import com.fantok.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val urlInput: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val apiService: ApiService
) : ViewModel() {

    val serverUrl = settingsDataStore.serverUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val autoPlay = settingsDataStore.autoPlay.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val loopVideo = settingsDataStore.loopVideo.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val rememberProgress = settingsDataStore.rememberProgress.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun onUrlInputChange(url: String) {
        _uiState.update { it.copy(urlInput = url, testResult = null) }
    }

    fun testConnection() {
        val url = _uiState.value.urlInput.trim().ifBlank { serverUrl.value }
        if (url.isBlank()) return
        _uiState.update { it.copy(isTesting = true, testResult = null) }
        baseUrlInterceptor.setBaseUrl(url)
        viewModelScope.launch {
            try {
                apiService.healthCheck()
                settingsDataStore.saveServerUrl(url)
                _uiState.update { it.copy(isTesting = false, testResult = "已连接", testSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTesting = false, testResult = "连接失败：${e.message}", testSuccess = false) }
                // Restore old URL on failure
                baseUrlInterceptor.setBaseUrl(serverUrl.value)
            }
        }
    }

    fun setAutoPlay(v: Boolean) { viewModelScope.launch { settingsDataStore.saveAutoPlay(v) } }
    fun setLoopVideo(v: Boolean) { viewModelScope.launch { settingsDataStore.saveLoopVideo(v) } }
    fun setRememberProgress(v: Boolean) { viewModelScope.launch { settingsDataStore.saveRememberProgress(v) } }
}
