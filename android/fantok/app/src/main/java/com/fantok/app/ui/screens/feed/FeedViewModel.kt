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

    /**
     * 延迟初始化，在界面准备好后再加载数据
     */
    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            loadVideos()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 分批加载视频，避免超时
                val batchSize = 500
                val firstResponse = apiService.getDouyinVideos(perPage = batchSize)
                val total = firstResponse.total

                if (total == 0) {
                    allVideos = emptyList()
                    _uiState.value = _uiState.value.copy(
                        playlist = emptyList(),
                        isLoading = false,
                        error = null
                    )
                    return@launch
                }

                // 先显示第一批
                val videos = firstResponse.items.toMutableList()
                allVideos = videos
                applyFilter()
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)

                // 后台加载剩余视频
                if (total > batchSize) {
                    val remainingPages = (total - 1) / batchSize
                    for (page in 2..remainingPages + 1) {
                        try {
                            val response = apiService.getDouyinVideos(page = page, perPage = batchSize)
                            videos.addAll(response.items)
                            allVideos = videos
                            // 只在当前筛选为全部时更新播放列表
                            if (_uiState.value.filterType == "all" && !_uiState.value.isRandom) {
                                _uiState.value = _uiState.value.copy(playlist = videos)
                            }
                        } catch (e: Exception) {
                            // 忽略加载错误，继续加载下一批
                        }
                    }
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
        // 在本地进行筛选和随机
        var filtered = allVideos

        // 筛选
        when (_uiState.value.filterType) {
            "liked" -> filtered = allVideos.filter { it.isLiked }
            "favorite" -> filtered = allVideos.filter { it.isFavorite }
        }

        // 随机排序
        val playlist = if (_uiState.value.isRandom && filtered.isNotEmpty()) {
            filtered.shuffled()
        } else {
            filtered
        }

        _uiState.value = _uiState.value.copy(
            playlist = playlist,
            currentIndex = 0
        )

        // Prepare first video
        if (playlist.isNotEmpty()) {
            playerManager.prepareAt(0, playlist)
        }
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
