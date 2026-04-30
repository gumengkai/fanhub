package com.fantok.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.Video
import com.fantok.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val playlist: List<Video> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterType: String = "all",
    val isRandom: Boolean = false,
    val showControls: Boolean = true
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val apiService: ApiService,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var allVideos: List<Video> = emptyList()
    private var isInitialized = false

    private var pendingStartVideoId: Int? = null

    /**
     * 延迟初始化，在界面准备好后再加载数据
     */
    fun initialize(startVideoId: Int? = null) {
        if (!isInitialized) {
            isInitialized = true
            pendingStartVideoId = startVideoId
            loadVideos()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 根据筛选条件从API获取数据
                val filterType = _uiState.value.filterType
                val response = when (filterType) {
                    "liked" -> apiService.getDouyinVideos(liked = true, perPage = 1000)
                    "favorite" -> apiService.getDouyinVideos(favorite = true, perPage = 1000)
                    else -> apiService.getDouyinVideos(perPage = 500)
                }

                val videos = response.items
                allVideos = videos

                // 随机排序
                val playlist = if (_uiState.value.isRandom && videos.isNotEmpty()) {
                    videos.shuffled()
                } else {
                    videos
                }

                // 查找指定视频的起始位置
                val startIndex = if (pendingStartVideoId != null && playlist.isNotEmpty()) {
                    val index = playlist.indexOfFirst { it.id == pendingStartVideoId }
                    if (index >= 0) index else 0
                } else 0
                pendingStartVideoId = null

                _uiState.value = _uiState.value.copy(
                    playlist = playlist,
                    currentIndex = startIndex,
                    isLoading = false,
                    error = null
                )

                // Prepare video at startIndex
                if (playlist.isNotEmpty()) {
                    playerManager.prepareAt(startIndex, playlist)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    private fun applyFilter() {
        // 重新加载数据（API筛选）
        loadVideos()
    }

    fun setFilterType(type: String) {
        _uiState.value = _uiState.value.copy(filterType = type)
        applyFilter()
    }

    fun toggleRandomMode() {
        _uiState.value = _uiState.value.copy(isRandom = !_uiState.value.isRandom)
        applyFilter()
    }

    fun toggleControlsVisibility() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(showControls = false)
    }

    fun onPageSettled(page: Int) {
        if (page != _uiState.value.currentIndex && page >= 0 && page < _uiState.value.playlist.size) {
            _uiState.value = _uiState.value.copy(currentIndex = page)
            playerManager.prepareAt(page, _uiState.value.playlist)
        }
    }

    fun toggleLike(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.toggleLike(videoId)
                val updatedList = _uiState.value.playlist.map { v ->
                    if (v.id == videoId) v.copy(isLiked = !v.isLiked) else v
                }
                _uiState.value = _uiState.value.copy(playlist = updatedList)
                allVideos = allVideos.map { v ->
                    if (v.id == videoId) v.copy(isLiked = !v.isLiked) else v
                }
            } catch (e: Exception) { }
        }
    }

    fun toggleFavorite(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.toggleFavorite(videoId)
                val updatedList = _uiState.value.playlist.map { v ->
                    if (v.id == videoId) v.copy(isFavorite = !v.isFavorite) else v
                }
                _uiState.value = _uiState.value.copy(playlist = updatedList)
                allVideos = allVideos.map { v ->
                    if (v.id == videoId) v.copy(isFavorite = !v.isFavorite) else v
                }
            } catch (e: Exception) { }
        }
    }

    fun deleteVideo(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.deleteVideo(videoId)
                allVideos = allVideos.filter { it.id != videoId }
                applyFilter()
            } catch (e: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
