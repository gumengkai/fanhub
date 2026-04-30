package com.fantok.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.Video
import com.fantok.app.data.repository.VideoCacheRepository
import com.fantok.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val allVideos: List<Video> = emptyList(),      // 所有视频（从本地缓存）
    val playlist: List<Video> = emptyList(),       // 当前播放列表（筛选后）
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,                // 后台同步中
    val error: String? = null,
    val filterType: String = "all",                // all/liked/favorite
    val isRandom: Boolean = false,
    val showControls: Boolean = true,
    val totalCount: Int = 0                      // 总视频数
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val apiService: ApiService,
    private val videoCacheRepository: VideoCacheRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var isInitialized = false
    private var pendingStartVideoId: Int? = null

    /**
     * 初始化：先加载本地缓存，再后台同步
     */
    fun initialize(startVideoId: Int? = null) {
        if (!isInitialized) {
            isInitialized = true
            pendingStartVideoId = startVideoId
            loadFromCache()
            // 后台同步最新数据
            syncFromServer()
        }
    }

    /**
     * 重置并重新初始化（用于导航时重新加载）
     */
    fun resetAndInitialize(filterType: String, startVideoId: Int? = null) {
        isInitialized = false
        _uiState.update { it.copy(filterType = filterType) }
        initialize(startVideoId)
    }

    /**
     * 从本地缓存加载视频
     */
    private fun loadFromCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 从本地缓存获取所有视频
                val cachedVideos = videoCacheRepository.getCachedVideos("all")

                if (cachedVideos.isEmpty()) {
                    // 缓存为空，等待服务器同步
                    _uiState.update {
                        it.copy(
                            allVideos = emptyList(),
                            playlist = emptyList(),
                            isLoading = true, // 保持loading状态等待同步
                            error = null
                        )
                    }
                } else {
                    // 使用缓存数据
                    _uiState.update {
                        it.copy(
                            allVideos = cachedVideos,
                            totalCount = cachedVideos.size,
                            isLoading = false
                        )
                    }
                    applyFilter(keepCurrentVideo = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 从服务器同步数据到本地缓存（分批加载）
     */
    private fun syncFromServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                // 先获取总数
                val firstResponse = apiService.getDouyinVideos(perPage = 1)
                val total = firstResponse.total

                if (total == 0) {
                    videoCacheRepository.clearCache()
                    _uiState.update {
                        it.copy(
                            allVideos = emptyList(),
                            playlist = emptyList(),
                            isLoading = false,
                            isSyncing = false,
                            totalCount = 0
                        )
                    }
                    return@launch
                }

                // 分批加载（每批500个）
                val batchSize = 500
                val allVideos = mutableListOf<Video>()
                val totalPages = (total + batchSize - 1) / batchSize

                for (page in 1..totalPages) {
                    try {
                        val response = apiService.getDouyinVideos(
                            page = page,
                            perPage = batchSize
                        )
                        allVideos.addAll(response.items)

                        // 保存到本地缓存
                        videoCacheRepository.saveVideos(allVideos.toList())

                        // 更新UI（首次加载时）
                        if (page == 1 || _uiState.value.allVideos.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    allVideos = allVideos.toList(),
                                    totalCount = total
                                )
                            }
                            applyFilter(keepCurrentVideo = null)
                        }
                    } catch (e: Exception) {
                        // 继续加载下一批
                    }
                }

                // 最终更新
                _uiState.update {
                    it.copy(
                        allVideos = allVideos.toList(),
                        totalCount = total,
                        isLoading = false,
                        isSyncing = false
                    )
                }
                applyFilter(keepCurrentVideo = null)

            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    /**
     * 应用筛选条件（本地筛选）
     */
    private fun applyFilter(keepCurrentVideo: Video? = null) {
        val allVideos = _uiState.value.allVideos
        val filterType = _uiState.value.filterType
        val isRandom = _uiState.value.isRandom

        // 本地筛选
        val filtered = when (filterType) {
            "liked" -> allVideos.filter { it.isLiked }
            "favorite" -> allVideos.filter { it.isFavorite }
            else -> allVideos
        }

        // 随机排序
        val playlist = if (isRandom && filtered.isNotEmpty()) {
            filtered.shuffled()
        } else {
            filtered
        }

        // 计算新的当前索引
        val newIndex = if (keepCurrentVideo != null && playlist.isNotEmpty()) {
            playlist.indexOfFirst { it.id == keepCurrentVideo.id }.coerceAtLeast(0)
        } else if (pendingStartVideoId != null && playlist.isNotEmpty()) {
            val index = playlist.indexOfFirst { it.id == pendingStartVideoId }
            pendingStartVideoId = null
            if (index >= 0) index else 0
        } else {
            0
        }

        _uiState.update {
            it.copy(
                playlist = playlist,
                currentIndex = newIndex
            )
        }

        // 准备播放
        if (playlist.isNotEmpty()) {
            playerManager.prepareAt(newIndex, playlist)
        }
    }

    /**
     * 设置筛选类型
     */
    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
        applyFilter(keepCurrentVideo = null)
    }

    /**
     * 切换随机/顺序模式
     */
    fun toggleRandomMode() {
        val state = _uiState.value
        val newRandom = !state.isRandom

        if (newRandom) {
            // 开启随机：记住当前视频，打乱后保持播放
            val currentVideo = state.playlist.getOrNull(state.currentIndex)
            val filtered = when (state.filterType) {
                "liked" -> state.allVideos.filter { it.isLiked }
                "favorite" -> state.allVideos.filter { it.isFavorite }
                else -> state.allVideos
            }
            val shuffled = if (filtered.isNotEmpty()) filtered.shuffled() else filtered
            val newIndex = if (currentVideo != null && shuffled.isNotEmpty()) {
                shuffled.indexOfFirst { it.id == currentVideo.id }.coerceAtLeast(0)
            } else 0

            _uiState.update {
                it.copy(
                    isRandom = true,
                    playlist = shuffled,
                    currentIndex = newIndex
                )
            }
            if (shuffled.isNotEmpty()) {
                playerManager.prepareAt(newIndex, shuffled)
            }
        } else {
            // 关闭随机：恢复原始顺序
            applyFilter(keepCurrentVideo = state.playlist.getOrNull(state.currentIndex))
        }
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun onPageSettled(page: Int) {
        val playlist = _uiState.value.playlist
        val currentIndex = _uiState.value.currentIndex
        if (page < 0 || page >= playlist.size) return

        if (page != currentIndex) {
            _uiState.update { it.copy(currentIndex = page) }
            playerManager.prepareAt(page, playlist)
        }
    }

    /**
     * 喜欢/取消喜欢
     */
    fun toggleLike(videoId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.toggleLike(videoId)
                if (response.isSuccessful) {
                    val isLiked = (response.body()?.get("is_liked") as? Boolean) ?: false
                    // 更新本地缓存
                    videoCacheRepository.updateLikeStatus(videoId, isLiked)
                    // 更新UI状态
                    _uiState.update { state ->
                        val updatedAllVideos = state.allVideos.map { video ->
                            if (video.id == videoId) video.copy(isLiked = isLiked) else video
                        }
                        val updatedPlaylist = state.playlist.map { video ->
                            if (video.id == videoId) video.copy(isLiked = isLiked) else video
                        }
                        state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    /**
     * 收藏/取消收藏
     */
    fun toggleFavorite(videoId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.toggleFavorite(videoId)
                if (response.isSuccessful) {
                    val isFavorite = (response.body()?.get("is_favorite") as? Boolean) ?: false
                    // 更新本地缓存
                    videoCacheRepository.updateFavoriteStatus(videoId, isFavorite)
                    // 更新UI状态
                    _uiState.update { state ->
                        val updatedAllVideos = state.allVideos.map { video ->
                            if (video.id == videoId) video.copy(isFavorite = isFavorite) else video
                        }
                        val updatedPlaylist = state.playlist.map { video ->
                            if (video.id == videoId) video.copy(isFavorite = isFavorite) else video
                        }
                        state.copy(allVideos = updatedAllVideos, playlist = updatedPlaylist)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    /**
     * 删除视频
     */
    fun deleteVideo(videoId: Int) {
        viewModelScope.launch {
            try {
                apiService.deleteVideo(videoId)
                // 从本地缓存删除
                val updatedAllVideos = _uiState.value.allVideos.filter { it.id != videoId }
                videoCacheRepository.saveVideos(updatedAllVideos)
                // 更新UI
                _uiState.update { state ->
                    val updatedPlaylist = state.playlist.filter { it.id != videoId }
                    val newIndex = if (state.currentIndex >= updatedPlaylist.size) 0 else state.currentIndex
                    state.copy(
                        allVideos = updatedAllVideos,
                        playlist = updatedPlaylist,
                        currentIndex = newIndex,
                        totalCount = updatedAllVideos.size
                    )
                }
                // 播放下一个
                val playlist = _uiState.value.playlist
                if (playlist.isNotEmpty()) {
                    playerManager.prepareAt(_uiState.value.currentIndex, playlist)
                }
            } catch (e: Exception) { }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        syncFromServer()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}