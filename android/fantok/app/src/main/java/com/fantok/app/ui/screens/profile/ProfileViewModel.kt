package com.fantok.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.Video
import com.fantok.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val likedVideos: List<Video> = emptyList(),
    val favoriteVideos: List<Video> = emptyList(),
    val likedCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = false))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var isInitialized = false

    /**
     * 延迟初始化，在界面准备好后再加载数据
     */
    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            loadData()
        }
    }

    fun getServerUrl(): String {
        return settingsDataStore.getServerUrl()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val likedResponse = apiService.getDouyinVideos(liked = true, perPage = 1000)
                val favoriteResponse = apiService.getDouyinVideos(favorite = true, perPage = 1000)
                val stats = apiService.getDouyinStats()

                _uiState.value = ProfileUiState(
                    likedVideos = likedResponse.items,
                    favoriteVideos = favoriteResponse.items,
                    likedCount = stats.liked,
                    favoriteCount = stats.favorite,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}