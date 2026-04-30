package com.fantok.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.Video
import com.fantok.app.data.local.SettingsDataStore
import com.fantok.app.data.repository.VideoCacheRepository
import com.fantok.app.data.repository.toVideo
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
    private val settingsDataStore: SettingsDataStore,
    private val videoCacheRepository: VideoCacheRepository
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
            loadFromCache()
        }
    }

    fun getServerUrl(): String {
        return settingsDataStore.getServerUrl()
    }

    /**
     * 从本地缓存加载数据
     */
    private fun loadFromCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 从本地缓存获取喜欢/收藏的视频
                val likedVideos = videoCacheRepository.getLikedVideos().first().map { it.toVideo() }
                val favoriteVideos = videoCacheRepository.getFavoriteVideos().first().map { it.toVideo() }
                val likedCount = videoCacheRepository.getLikedCount()
                val favoriteCount = videoCacheRepository.getFavoriteCount()

                _uiState.value = ProfileUiState(
                    likedVideos = likedVideos,
                    favoriteVideos = favoriteVideos,
                    likedCount = likedCount,
                    favoriteCount = favoriteCount,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}