package com.fanhub.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanhub.app.data.api.ApiService
import com.fanhub.app.data.model.Video
import com.fanhub.app.data.model.Tag
import com.fanhub.app.data.repository.HistoryRepository
import com.fanhub.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val allVideos: List<Video> = emptyList(),      // 所有视频（原始数据）
    val playlist: List<Video> = emptyList(),       // 当前播放列表（筛选后）
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRandom: Boolean = false,                 // 随机模式
    val filterType: String = "all",                // all/liked/favorite/unwatched/tag
    val selectedTagId: Int? = null,                // 选中标签ID
    val tags: List<Tag> = emptyList(),             // 标签列表
    val watchedVideoIds: Set<Int> = emptySet()     // 已观看的视频ID集合
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val apiService: ApiService,
    private val historyRepository: HistoryRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAllVideos()
        loadTags()
        loadWatchedVideos()
    }

    /** 获取所有视频（参考web端，先获取第一页知道总数，再获取全部） */
    private fun loadAllVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 先获取第一页以知道总数
                val firstResponse = apiService.getVideos(page = 1, perPage = 1)
                val total = firstResponse.total

                if (total == 0) {
                    _uiState.update { it.copy(allVideos = emptyList(), playlist = emptyList(), isLoading = false) }
                    return@launch
                }

                // 获取全部视频
                val response = apiService.getVideos(page = 1, perPage = total)
                val videos = response.items

                _uiState.update { it.copy(allVideos = videos, isLoading = false) }
                applyFilter()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** 加载标签列表 */
    private fun loadTags() {
        viewModelScope.launch {
            try {
                val response = apiService.getTags()
                _uiState.update { it.copy(tags = response.items ?: emptyList()) }
            } catch (e: Exception) {
                // Ignore tag loading error
            }
        }
    }

    /** 应用筛选条件 */
    fun applyFilter() {
        val allVideos = _uiState.value.allVideos
        val filterType = _uiState.value.filterType
        val selectedTagId = _uiState.value.selectedTagId
        val isRandom = _uiState.value.isRandom
        val watchedVideoIds = _uiState.value.watchedVideoIds

        val filtered = when (filterType) {
            "liked" -> allVideos.filter { it.isLiked }
            "favorite" -> allVideos.filter { it.isFavorite }
            "unwatched" -> allVideos.filter { !watchedVideoIds.contains(it.id) }
            "tag" -> if (selectedTagId != null) {
                allVideos.filter { it.tags.any { tag -> tag.id == selectedTagId } }
            } else {
                allVideos
            }
            else -> allVideos
        }

        val playlist = if (isRandom && filtered.isNotEmpty()) filtered.shuffled() else filtered
        
        _uiState.update { it.copy(playlist = playlist, currentIndex = 0) }

        // 播放第一个视频
        if (playlist.isNotEmpty()) {
            playerManager.prepareAt(0, playlist)
        }
    }

    /** 设置筛选类型 */
    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
        applyFilter()
    }

    /** 设置选中标签 */
    fun setSelectedTag(tagId: Int?) {
        _uiState.update { it.copy(selectedTagId = tagId) }
        applyFilter()
    }

    /** 切换随机/顺序模式 */
    fun toggleRandomMode() {
        val state = _uiState.value
        val newRandom = !state.isRandom
        
        if (newRandom) {
            // 开启随机：记住当前视频，打乱后保持播放
            val currentVideo = state.playlist.getOrNull(state.currentIndex)
            val shuffled = state.playlist.shuffled()
            val newIndex = if (currentVideo != null) {
                shuffled.indexOfFirst { it.id == currentVideo.id }.coerceAtLeast(0)
            } else 0
            
            _uiState.update { it.copy(isRandom = true, playlist = shuffled, currentIndex = newIndex) }
            if (shuffled.isNotEmpty()) {
                playerManager.prepareAt(newIndex, shuffled)
            }
        } else {
            // 关闭随机：恢复原始顺序，记住当前视频
            val currentVideo = state.playlist.getOrNull(state.currentIndex)
            applyFilterInternal(keepCurrentVideo = currentVideo)
        }
    }

    /** 内部筛选方法，可选择保持当前视频 */
    private fun applyFilterInternal(keepCurrentVideo: Video? = null) {
        val allVideos = _uiState.value.allVideos
        val filterType = _uiState.value.filterType
        val selectedTagId = _uiState.value.selectedTagId
        val isRandom = _uiState.value.isRandom
        val watchedVideoIds = _uiState.value.watchedVideoIds

        val filtered = when (filterType) {
            "liked" -> allVideos.filter { it.isLiked }
            "favorite" -> allVideos.filter { it.isFavorite }
            "unwatched" -> allVideos.filter { !watchedVideoIds.contains(it.id) }
            "tag" -> if (selectedTagId != null) {
                allVideos.filter { it.tags.any { tag -> tag.id == selectedTagId } }
            } else {
                allVideos
            }
            else -> allVideos
        }

        val playlist = if (isRandom && filtered.isNotEmpty()) filtered.shuffled() else filtered
        
        val newIndex = if (keepCurrentVideo != null && playlist.isNotEmpty()) {
            playlist.indexOfFirst { it.id == keepCurrentVideo.id }.coerceAtLeast(0)
        } else 0
        
        _uiState.update { it.copy(playlist = playlist, currentIndex = newIndex) }

        if (playlist.isNotEmpty()) {
            playerManager.prepareAt(newIndex, playlist)
        }
    }

    /** 页面切换 settled */
    fun onPageSettled(index: Int) {
        val playlist = _uiState.value.playlist
        val currentIndex = _uiState.value.currentIndex
        if (index < 0 || index >= playlist.size) return
        
        // 只有页面真正改变时才更新
        if (index != currentIndex) {
            _uiState.update { it.copy(currentIndex = index) }
            playerManager.prepareAt(index, playlist)
        }
    }

    /** 下一个视频 */
    fun nextVideo() {
        val state = _uiState.value
        val playlist = state.playlist
        if (playlist.isEmpty()) return

        val nextIndex = (state.currentIndex + 1) % playlist.size
        _uiState.update { it.copy(currentIndex = nextIndex) }
        playerManager.prepareAt(nextIndex, playlist)
    }

    /** 上一个视频 */
    fun prevVideo() {
        val state = _uiState.value
        val playlist = state.playlist
        if (playlist.isEmpty()) return

        val prevIndex = (state.currentIndex - 1 + playlist.size) % playlist.size
        _uiState.update { it.copy(currentIndex = prevIndex) }
        playerManager.prepareAt(prevIndex, playlist)
    }

    /** 同步播放进度 */
    fun syncProgress(videoId: Int, progress: Float) {
        viewModelScope.launch {
            val duration = playerManager.player.duration
            val position = (progress * duration).toInt()
            historyRepository.updateProgress(videoId, position, duration.toInt())
            
            // 标记为已观看（只要有进度就算观看过）
            if (progress > 0.05f) {
                _uiState.update { state ->
                    state.copy(watchedVideoIds = state.watchedVideoIds + videoId)
                }
            }
        }
    }
    
    /** 加载观看历史 */
    private fun loadWatchedVideos() {
        viewModelScope.launch {
            try {
                val historyResult = historyRepository.getHistory()
                historyResult.onSuccess { response ->
                    @Suppress("UNCHECKED_CAST")
                    val items = response["items"] as? List<Map<String, Any>> ?: emptyList()
                    val watchedIds = items.mapNotNull { 
                        (it["video_id"] as? Number)?.toInt() 
                    }.toSet()
                    _uiState.update { it.copy(watchedVideoIds = watchedIds) }
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    /** 喜欢/取消喜欢 */
    fun toggleLike(videoId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.toggleLike(videoId)
                if (response.isSuccessful) {
                    val isLiked = (response.body()?.get("is_liked") as? Boolean) ?: false
                    _uiState.update { state ->
                        val updatedVideos = state.allVideos.map { video ->
                            if (video.id == videoId) video.copy(isLiked = isLiked) else video
                        }
                        val updatedPlaylist = state.playlist.map { video ->
                            if (video.id == videoId) video.copy(isLiked = isLiked) else video
                        }
                        state.copy(allVideos = updatedVideos, playlist = updatedPlaylist)
                    }
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    /** 收藏/取消收藏 */
    fun toggleFavorite(videoId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.toggleFavorite(videoId)
                if (response.isSuccessful) {
                    val isFavorite = (response.body()?.get("is_favorite") as? Boolean) ?: false
                    _uiState.update { state ->
                        val updatedVideos = state.allVideos.map { video ->
                            if (video.id == videoId) video.copy(isFavorite = isFavorite) else video
                        }
                        val updatedPlaylist = state.playlist.map { video ->
                            if (video.id == videoId) video.copy(isFavorite = isFavorite) else video
                        }
                        state.copy(allVideos = updatedVideos, playlist = updatedPlaylist)
                    }
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    /** 删除视频 */
    fun deleteVideo(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.deleteVideo(videoId)
                _uiState.update { state ->
                    val updatedAllVideos = state.allVideos.filter { it.id != videoId }
                    val updatedPlaylist = state.playlist.filter { it.id != videoId }
                    val newIndex = if (state.currentIndex >= updatedPlaylist.size) 0 else state.currentIndex
                    state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist, currentIndex = newIndex)
                }
                // 播放下一个
                val playlist = _uiState.value.playlist
                if (playlist.isNotEmpty()) {
                    playerManager.prepareAt(_uiState.value.currentIndex, playlist)
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    /** 刷新数据 */
    fun refresh() {
        loadAllVideos()
    }

    /** 更新视频信息（标题、描述、标签） */
    fun updateVideoInfo(videoId: Int, title: String, description: String, tagIds: List<Int>) {
        viewModelScope.launch {
            try {
                // 更新标题和描述
                apiService.updateVideo(videoId, mapOf("title" to title, "description" to description))

                // 获取当前视频的标签
                val currentTags = apiService.getVideoTags(videoId).items.map { it.id }

                // 移除不在新列表中的标签
                for (tagId in currentTags) {
                    if (!tagIds.contains(tagId)) {
                        apiService.removeTagFromVideo(videoId, tagId)
                    }
                }

                // 添加新标签
                for (tagId in tagIds) {
                    if (!currentTags.contains(tagId)) {
                        apiService.addTagToVideo(videoId, mapOf("tag_id" to tagId))
                    }
                }

                // 更新本地状态
                _uiState.update { state ->
                    val updatedTags = state.tags.filter { tagIds.contains(it.id) }
                    val updatedAllVideos = state.allVideos.map { video ->
                        if (video.id == videoId) {
                            video.copy(title = title, description = description, tags = updatedTags)
                        } else video
                    }
                    val updatedPlaylist = state.playlist.map { video ->
                        if (video.id == videoId) {
                            video.copy(title = title, description = description, tags = updatedTags)
                        } else video
                    }
                    state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist)
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    /** 只更新视频标签 */
    fun updateVideoTags(videoId: Int, tagIds: List<Int>) {
        viewModelScope.launch {
            try {
                // 获取当前视频的标签
                val currentTags = apiService.getVideoTags(videoId).items.map { it.id }

                // 移除不在新列表中的标签
                for (tagId in currentTags) {
                    if (!tagIds.contains(tagId)) {
                        apiService.removeTagFromVideo(videoId, tagId)
                    }
                }

                // 添加新标签
                for (tagId in tagIds) {
                    if (!currentTags.contains(tagId)) {
                        apiService.addTagToVideo(videoId, mapOf("tag_id" to tagId))
                    }
                }

                // 更新本地状态
                _uiState.update { state ->
                    val updatedTags = state.tags.filter { tagIds.contains(it.id) }
                    val updatedAllVideos = state.allVideos.map { video ->
                        if (video.id == videoId) video.copy(tags = updatedTags) else video
                    }
                    val updatedPlaylist = state.playlist.map { video ->
                        if (video.id == videoId) video.copy(tags = updatedTags) else video
                    }
                    state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist)
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }
}