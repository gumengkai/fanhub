package com.fanhub.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanhub.app.data.api.ApiService
import com.fanhub.app.data.api.BaseUrlInterceptor
import com.fanhub.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val url: String = "",
    val isLoading: Boolean = false,
    val connected: Boolean = false,
    val errorMsg: String? = null,
    val navigateAway: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun onUrlChange(url: String) {
        _uiState.update { it.copy(url = url, errorMsg = null, connected = false) }
    }

    fun testAndSave() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return

        _uiState.update { it.copy(isLoading = true, errorMsg = null, connected = false) }
        baseUrlInterceptor.setBaseUrl(url)

        viewModelScope.launch {
            try {
                apiService.healthCheck()
                settingsDataStore.saveServerUrl(url)
                _uiState.update { it.copy(isLoading = false, connected = true, navigateAway = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMsg = "无法连接：${e.message}") }
            }
        }
    }
}
