package com.fanpeak.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanpeak.app.data.api.ApiService
import com.fanpeak.app.data.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val myFavorites: List<Video> = emptyList(),        // 我的收藏（首页主要展示）
    val todayRecommend: List<Video> = emptyList(),     // 今日推荐
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = false))
    val uiState = _uiState.asStateFlow()

    private var isInitialized = false

    /**
     * 延迟加载数据，在界面准备好后再调用
     */
    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            loadData()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. 获取收藏视频（首页主要展示）
                val favoritesResp = apiService.getVideos(
                    page = 1,
                    perPage = 100,  // 多取一些用于随机
                    favorite = true
                )
                // 随机打乱取20个
                val favorites = favoritesResp.items.shuffled().take(20)
                _uiState.update { it.copy(myFavorites = favorites) }

                // 2. 生成今日推荐
                val recommendations = generateTodayRecommend(favorites)
                _uiState.update { it.copy(todayRecommend = recommendations) }

                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "无法连接服务器: ${e.message}"
                ) }
            }
        }
    }

    /** 
     * 生成今日推荐
     * 算法：
     * 1. 从收藏视频中随机选择几个作为种子
     * 2. 获取这些视频的相关视频
     * 3. 合并去重，排除已收藏的
     * 4. 随机选择10个
     */
    private suspend fun generateTodayRecommend(favorites: List<Video>): List<Video> {
        if (favorites.isEmpty()) {
            // 没有收藏时，随机推荐播放量高的视频
            return try {
                val resp = apiService.getVideos(
                    page = 1,
                    perPage = 20,
                    sortBy = "view_count",
                    order = "desc"
                )
                resp.items.shuffled().take(10)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // 从收藏中随机选 3 个种子视频
        val seedVideos = favorites.shuffled().take(3)
        val favoriteIds = favorites.map { it.id }.toSet()
        
        val relatedVideos = mutableListOf<Video>()
        
        // 获取每个种子视频的相关视频
        for (seed in seedVideos) {
            try {
                val relatedResp = apiService.getRelatedVideos(seed.id)
                relatedVideos.addAll(relatedResp.items)
            } catch (e: Exception) {
                // 忽略单个失败
            }
        }

        // 去重，排除已收藏的，随机取10个
        return relatedVideos
            .distinctBy { it.id }
            .filter { it.id !in favoriteIds }
            .shuffled()
            .take(10)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
