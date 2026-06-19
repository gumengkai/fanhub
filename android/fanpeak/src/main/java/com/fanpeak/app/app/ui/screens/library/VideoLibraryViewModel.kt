package com.fanpeak.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanpeak.app.data.model.Tag
import com.fanpeak.app.data.model.Video
import com.fanpeak.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 筛选类型枚举
enum class FilterType {
    ALL,       // 全部
    LIKED,     // 已喜欢
    FAVORITE,  // 已收藏
    UNWATCHED  // 未观看
}

// 排序选项
enum class SortOption(val sortBy: String, val order: String, val label: String) {
    CREATED_AT_DESC("created_at", "desc", "最新添加"),
    CREATED_AT_ASC("created_at", "asc", "最早添加"),
    TITLE_ASC("title", "asc", "名称 A-Z"),
    TITLE_DESC("title", "desc", "名称 Z-A"),
    FILE_SIZE_DESC("file_size", "desc", "文件大小 (大→小)"),
    FILE_SIZE_ASC("file_size", "asc", "文件大小 (小→大)")
}

data class LibraryUiState(
    val videos: List<Video> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Int? = null,
    val sortOption: SortOption = SortOption.CREATED_AT_DESC,
    val filterType: FilterType = FilterType.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 1,
    val total: Int = 0,
    // 操作状态
    val isDeleting: Boolean = false,
    val deletingVideoId: Int? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VideoLibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = false))
    val uiState = _uiState.asStateFlow()

    private var isInitialized = false

    /**
     * 延迟初始化，在界面准备好后再调用
     */
    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            loadTags()
            loadVideos(reset = true)
        }
    }

    private fun loadTags() {
        // Tags feature removed - do nothing
    }

    fun selectTag(tagId: Int?) {
        _uiState.update { it.copy(selectedTagId = tagId) }
        loadVideos(reset = true)
    }

    fun setSort(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        loadVideos(reset = true)
    }

    fun setFilter(filterType: FilterType) {
        _uiState.update { it.copy(filterType = filterType) }
        loadVideos(reset = true)
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadVideos(reset = true)
    }

    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(video.id)
                .onSuccess { isFavorite ->
                    _uiState.update { state ->
                        state.copy(
                            videos = state.videos.map { v ->
                                if (v.id == video.id) v.copy(isFavorite = isFavorite) else v
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "收藏操作失败: ${e.message}") }
                }
        }
    }

    fun toggleLike(video: Video) {
        viewModelScope.launch {
            mediaRepository.toggleLike(video.id)
                .onSuccess { isLiked ->
                    _uiState.update { state ->
                        state.copy(
                            videos = state.videos.map { v ->
                                if (v.id == video.id) v.copy(isLiked = isLiked) else v
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "喜欢操作失败: ${e.message}") }
                }
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deletingVideoId = video.id) }
            mediaRepository.deleteVideo(video.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            videos = state.videos.filter { it.id != video.id },
                            total = state.total - 1,
                            isDeleting = false,
                            deletingVideoId = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        isDeleting = false,
                        deletingVideoId = null,
                        errorMessage = "删除失败: ${e.message}"
                    ) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadVideos(reset: Boolean = false) {
        val state = _uiState.value
        if (!reset && (!state.hasMore || state.isLoadingMore)) return

        val page = if (reset) 1 else state.page
        _uiState.update {
            if (reset) it.copy(isLoading = true, videos = emptyList(), page = 1, hasMore = true)
            else it.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            val favorite = when (state.filterType) {
                FilterType.FAVORITE -> true
                else -> null
            }
            val liked = when (state.filterType) {
                FilterType.LIKED -> true
                else -> null
            }
            // For unwatched filter, we pass watched=false to get unwatched videos
            val unwatched = when (state.filterType) {
                FilterType.UNWATCHED -> false
                else -> null
            }

            mediaRepository.getVideos(
                page = page,
                perPage = 20,
                sortBy = state.sortOption.sortBy,
                order = state.sortOption.order,
                favorite = favorite,
                liked = liked,
                unwatched = unwatched,
                search = state.searchQuery
            ).onSuccess { resp ->
                _uiState.update {
                    val newVideos = if (reset) resp.items else (it.videos + resp.items).distinctBy { v -> v.id }
                    it.copy(
                        videos = newVideos,
                        isLoading = false,
                        isLoadingMore = false,
                        page = page + 1,
                        hasMore = resp.currentPage < resp.pages,
                        total = resp.total
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }
}