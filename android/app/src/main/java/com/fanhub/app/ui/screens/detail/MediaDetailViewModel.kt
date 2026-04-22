package com.fanhub.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.fanhub.app.data.api.ApiService
import com.fanhub.app.data.model.Tag
import com.fanhub.app.data.model.Video
import com.fanhub.app.data.repository.MediaRepository
import com.fanhub.app.data.repository.HistoryRepository
import com.fanhub.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val video: Video? = null,
    val isLoading: Boolean = false,
    val allTags: List<Tag> = emptyList()
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val historyRepository: HistoryRepository,
    private val playerManager: PlayerManager,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()

    val player: ExoPlayer get() = playerManager.player

    fun load(videoId: Int) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 加载视频信息
            mediaRepository.getVideo(videoId)
                .onSuccess { video ->
                    _uiState.update { it.copy(video = video, isLoading = false) }
                    val uri = playerManager.buildStreamUri(video.id)
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.playWhenReady = true
                }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }

            // 加载所有标签
            loadTags()
        }
    }

    private suspend fun loadTags() {
        try {
            val tags = apiService.getTags().items ?: emptyList()
            _uiState.update { it.copy(allTags = tags) }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun toggleFavorite() {
        val videoId = _uiState.value.video?.id ?: return
        viewModelScope.launch {
            mediaRepository.toggleFavorite(videoId)
                .onSuccess { isFavorite ->
                    _uiState.update { state ->
                        state.video?.let { video ->
                            state.copy(video = video.copy(isFavorite = isFavorite))
                        } ?: state
                    }
                }
        }
    }

    fun toggleLike() {
        val videoId = _uiState.value.video?.id ?: return
        viewModelScope.launch {
            mediaRepository.toggleLike(videoId)
                .onSuccess { isLiked ->
                    _uiState.update { state ->
                        state.video?.let { video ->
                            state.copy(video = video.copy(isLiked = isLiked))
                        } ?: state
                    }
                }
        }
    }

    fun updateVideoInfo(title: String, description: String, tagIds: List<Int>) {
        val video = _uiState.value.video ?: return
        viewModelScope.launch {
            try {
                // 更新标题和描述
                apiService.updateVideo(video.id, mapOf("title" to title, "description" to description))

                // 获取当前视频的标签
                val currentTags = apiService.getVideoTags(video.id).items.map { it.id }

                // 移除不在新列表中的标签
                for (tagId in currentTags) {
                    if (!tagIds.contains(tagId)) {
                        apiService.removeTagFromVideo(video.id, tagId)
                    }
                }

                // 添加新标签
                for (tagId in tagIds) {
                    if (!currentTags.contains(tagId)) {
                        apiService.addTagToVideo(video.id, mapOf("tag_id" to tagId))
                    }
                }

                // 更新本地状态
                val updatedTags = _uiState.value.allTags.filter { tagIds.contains(it.id) }
                _uiState.update { state ->
                    state.video?.let { v ->
                        state.copy(video = v.copy(title = title, description = description, tags = updatedTags))
                    } ?: state
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    fun deleteVideo() {
        val videoId = _uiState.value.video?.id ?: return
        viewModelScope.launch {
            mediaRepository.deleteVideo(videoId)
        }
    }

    fun releasePlayer() {
        player.stop()
        player.clearMediaItems()
    }
}