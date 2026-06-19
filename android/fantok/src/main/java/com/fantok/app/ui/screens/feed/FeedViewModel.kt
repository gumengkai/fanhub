package com.fantok.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.Video
import com.fantok.app.data.repository.HistoryRepository
import com.fantok.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val allVideos: List<Video> = emptyList(),
    val playlist: List<Video> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRandom: Boolean = false,
    val filterType: String = "all",
    val watchedVideoIds: Set<Int> = emptySet(),
    val currentVideoRequiresExternalPlayer: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val apiService: ApiService,
    private val historyRepository: HistoryRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    private var isInitialized = false

    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            loadAllVideos()
            loadWatchedVideos()
        }
    }

    private fun loadAllVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val firstResponse = apiService.getVideos(page = 1, perPage = 1)
                val total = firstResponse.total

                if (total == 0) {
                    _uiState.update { it.copy(allVideos = emptyList(), playlist = emptyList(), isLoading = false) }
                    return@launch
                }

                val response = apiService.getVideos(page = 1, perPage = total)
                val videos = response.items

                _uiState.update { it.copy(allVideos = videos, isLoading = false) }
                applyFilter()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun applyFilter() {
        val allVideos = _uiState.value.allVideos
        val filterType = _uiState.value.filterType
        val isRandom = _uiState.value.isRandom
        val watchedVideoIds = _uiState.value.watchedVideoIds

        val filtered = when (filterType) {
            "liked" -> allVideos.filter { it.isLiked }
            "favorite" -> allVideos.filter { it.isFavorite }
            "unwatched" -> allVideos.filter { !watchedVideoIds.contains(it.id) }
            else -> allVideos
        }

        val playlist = if (isRandom && filtered.isNotEmpty()) filtered.shuffled() else filtered
        
        _uiState.update { it.copy(playlist = playlist, currentIndex = 0) }

        if (playlist.isNotEmpty()) {
            val requiresExternal = playerManager.prepareAt(0, playlist)
            _uiState.update { it.copy(currentVideoRequiresExternalPlayer = requiresExternal) }
        }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
        applyFilter()
    }

    fun toggleRandomMode() {
        val state = _uiState.value
        val newRandom = !state.isRandom
        
        if (newRandom) {
            val currentVideo = state.playlist.getOrNull(state.currentIndex)
            val shuffled = state.playlist.shuffled()
            val newIndex = if (currentVideo != null) {
                shuffled.indexOfFirst { it.id == currentVideo.id }.coerceAtLeast(0)
            } else 0
            
            _uiState.update { it.copy(isRandom = true, playlist = shuffled, currentIndex = newIndex) }
            if (shuffled.isNotEmpty()) {
                val requiresExternal = playerManager.prepareAt(newIndex, shuffled)
                _uiState.update { it.copy(currentVideoRequiresExternalPlayer = requiresExternal) }
            }
        } else {
            val currentVideo = state.playlist.getOrNull(state.currentIndex)
            val original = state.allVideos.filter { video ->
                when (state.filterType) {
                    "liked" -> video.isLiked
                    "favorite" -> video.isFavorite
                    "unwatched" -> !state.watchedVideoIds.contains(video.id)
                    else -> true
                }
            }
            val newIndex = if (currentVideo != null) {
                original.indexOfFirst { it.id == currentVideo.id }.coerceAtLeast(0)
            } else 0
            
            _uiState.update { it.copy(isRandom = false, playlist = original, currentIndex = newIndex) }
            if (original.isNotEmpty()) {
                val requiresExternal = playerManager.prepareAt(newIndex, original)
                _uiState.update { it.copy(currentVideoRequiresExternalPlayer = requiresExternal) }
            }
        }
    }

    private fun loadWatchedVideos() {
        viewModelScope.launch {
            try {
                val watchedIds = historyRepository.getAllWatchedVideoIds()
                _uiState.update { it.copy(watchedVideoIds = watchedIds) }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleLike(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.toggleLike(videoId)
                _uiState.update { state ->
                    val updatedAllVideos = state.allVideos.map { video ->
                        if (video.id == videoId) video.copy(isLiked = !video.isLiked) else video
                    }
                    val updatedPlaylist = state.playlist.map { video ->
                        if (video.id == videoId) video.copy(isLiked = !video.isLiked) else video
                    }
                    state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleFavorite(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.toggleFavorite(videoId)
                _uiState.update { state ->
                    val updatedAllVideos = state.allVideos.map { video ->
                        if (video.id == videoId) video.copy(isFavorite = !video.isFavorite) else video
                    }
                    val updatedPlaylist = state.playlist.map { video ->
                        if (video.id == videoId) video.copy(isFavorite = !video.isFavorite) else video
                    }
                    state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun deleteVideo(videoId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteVideo(videoId)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        val updatedAllVideos = state.allVideos.filter { it.id != videoId }
                        val updatedPlaylist = state.playlist.filter { it.id != videoId }
                        val newIndex = state.currentIndex.coerceIn(0, (updatedPlaylist.size - 1).coerceAtLeast(0))
                        
                        // 更新播放器状态
                        val requiresExternal = if (updatedPlaylist.isNotEmpty() && newIndex < updatedPlaylist.size) {
                            playerManager.prepareAt(newIndex, updatedPlaylist)
                        } else false
                        
                        state.copy(
                            allVideos = updatedAllVideos, 
                            playlist = updatedPlaylist, 
                            currentIndex = newIndex,
                            currentVideoRequiresExternalPlayer = requiresExternal
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "删除失败: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    fun onPageSettled(page: Int) {
        val playlist = _uiState.value.playlist
        var requiresExternal = false
        if (page >= 0 && page < playlist.size) {
            requiresExternal = playerManager.prepareAt(page, playlist)
        }
        _uiState.update { it.copy(currentIndex = page, currentVideoRequiresExternalPlayer = requiresExternal) }
    }
    
    /**
     * 使用外部播放器播放当前视频（用于不支持的视频格式）
     */
    fun playWithExternalPlayer() {
        val currentVideo = _uiState.value.playlist.getOrNull(_uiState.value.currentIndex)
        if (currentVideo != null) {
            playerManager.playWithExternalPlayer(currentVideo)
        }
    }

    fun nextVideo() {
        val state = _uiState.value
        if (state.currentIndex < state.playlist.size - 1) {
            onPageSettled(state.currentIndex + 1)
        }
    }

    fun prevVideo() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            onPageSettled(state.currentIndex - 1)
        }
    }

    fun syncProgress(videoId: Int, progress: Float) {
        viewModelScope.launch {
            try {
                historyRepository.saveProgress(videoId, progress)
            } catch (e: Exception) {
            }
        }
    }
}
