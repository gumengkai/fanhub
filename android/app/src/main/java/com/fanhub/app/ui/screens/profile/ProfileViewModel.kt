package com.fanhub.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanhub.app.data.api.ApiService
import com.fanhub.app.data.local.SettingsDataStore
import com.fanhub.app.data.model.parseVideoResponse
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
    val thumbnailUrl: String,
    val mediaType: String // "video" or "image"
)

data class ProfileUiState(
    val likedVideos: List<ProfileMediaItem> = emptyList(),
    val likedImages: List<ProfileMediaItem> = emptyList(),
    val favoriteVideos: List<ProfileMediaItem> = emptyList(),
    val favoriteImages: List<ProfileMediaItem> = emptyList(),
    val likedVideoCount: Int = 0,
    val likedImageCount: Int = 0,
    val favoriteVideoCount: Int = 0,
    val favoriteImageCount: Int = 0,
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
            // 获取喜欢统计
            val likesStatsResponse = apiService.getLikesStats()
            if (likesStatsResponse.isSuccessful) {
                val stats = likesStatsResponse.body()
                val videoCount = (stats?.get("video_count") as? Number)?.toInt() ?: 0
                val imageCount = (stats?.get("image_count") as? Number)?.toInt() ?: 0

                // 获取视频列表
                val likesResponse = apiService.getLikes()
                if (likesResponse.isSuccessful) {
                    val body = likesResponse.body()
                    val items = (body?.get("items") as? List<Map<String, Any>>) ?: emptyList()

                    val videos = items.filter { it["media_type"] == "video" }.map { item ->
                        val video = parseVideoResponse(item)
                        ProfileMediaItem(
                            id = video.id,
                            title = video.title,
                            thumbnailUrl = "$serverUrl/api/videos/${video.id}/thumbnail",
                            mediaType = "video"
                        )
                    }

                    val images = items.filter { it["media_type"] == "image" }.map { item ->
                        ProfileMediaItem(
                            id = (item["id"] as? Number)?.toInt() ?: 0,
                            title = item["title"] as? String ?: "",
                            thumbnailUrl = "$serverUrl/api/images/${(item["id"] as? Number)?.toInt()}/thumbnail",
                            mediaType = "image"
                        )
                    }

                    _uiState.update { it.copy(
                        likedVideos = videos,
                        likedImages = images,
                        likedVideoCount = videoCount,
                        likedImageCount = imageCount,
                        isLoading = false
                    ) }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadFavorites() {
        try {
            // 获取收藏统计
            val favStatsResponse = apiService.getFavoritesStats()
            if (favStatsResponse.isSuccessful) {
                val stats = favStatsResponse.body()
                val videoCount = (stats?.get("video_count") as? Number)?.toInt() ?: 0
                val imageCount = (stats?.get("image_count") as? Number)?.toInt() ?: 0

                // 获取收藏列表
                val favResponse = apiService.getFavorites()
                if (favResponse.isSuccessful) {
                    val body = favResponse.body()
                    val items = (body?.get("items") as? List<Map<String, Any>>) ?: emptyList()

                    val videos = items.filter { it["media_type"] == "video" }.map { item ->
                        val video = parseVideoResponse(item)
                        ProfileMediaItem(
                            id = video.id,
                            title = video.title,
                            thumbnailUrl = "$serverUrl/api/videos/${video.id}/thumbnail",
                            mediaType = "video"
                        )
                    }

                    val images = items.filter { it["media_type"] == "image" }.map { item ->
                        ProfileMediaItem(
                            id = (item["id"] as? Number)?.toInt() ?: 0,
                            title = item["title"] as? String ?: "",
                            thumbnailUrl = "$serverUrl/api/images/${(item["id"] as? Number)?.toInt()}/thumbnail",
                            mediaType = "image"
                        )
                    }

                    _uiState.update { it.copy(
                        favoriteVideos = videos,
                        favoriteImages = images,
                        favoriteVideoCount = videoCount,
                        favoriteImageCount = imageCount,
                        isLoading = false
                    ) }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}