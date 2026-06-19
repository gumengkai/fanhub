package com.fanpeak.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanpeak.app.data.api.ApiService
import com.fanpeak.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileMediaItem(
    val id: Int,
    val title: String,
    val thumbnailUrl: String
)

data class ProfileUiState(
    val likedVideos: List<ProfileMediaItem> = emptyList(),
    val favoriteVideos: List<ProfileMediaItem> = emptyList(),
    val likedCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var serverUrl: String = ""

    init {
        viewModelScope.launch {
            serverUrl = settingsDataStore.serverUrl.first()
            loadAll()
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 并行加载喜欢和收藏数据
            launch { loadLikes() }
            launch { loadFavorites() }
        }
    }

    private suspend fun loadLikes() {
        try {
            // 获取喜欢的抖音视频
            val likedResp = apiService.getVideos(
                page = 1,
                perPage = 100,
                liked = true
            )
            
            val videos = likedResp.items.map { video ->
                ProfileMediaItem(
                    id = video.id,
                    title = video.title,
                    thumbnailUrl = "$serverUrl/api/douyin/${video.id}/thumbnail"
                )
            }

            _uiState.update { it.copy(
                likedVideos = videos,
                likedCount = videos.size,
                isLoading = false
            ) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadFavorites() {
        try {
            // 获取收藏的抖音视频
            val favResp = apiService.getVideos(
                page = 1,
                perPage = 100,
                favorite = true
            )
            
            val videos = favResp.items.map { video ->
                ProfileMediaItem(
                    id = video.id,
                    title = video.title,
                    thumbnailUrl = "$serverUrl/api/douyin/${video.id}/thumbnail"
                )
            }

            _uiState.update { it.copy(
                favoriteVideos = videos,
                favoriteCount = videos.size,
                isLoading = false
            ) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
