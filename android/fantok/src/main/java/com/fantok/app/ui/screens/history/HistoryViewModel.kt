package com.fantok.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val grouped: Map<String, List<HistoryItem>> = emptyMap(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = false))
    val uiState = _uiState.asStateFlow()

    private var isInitialized = false

    /**
     * 延迟初始化，在界面准备好后再加载数据
     */
    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            historyRepository.getHistory()
                .onSuccess { data ->
                    val items = (data["items"] as? List<Map<String, Any>>)?.map { item ->
                        val video = (item["video"] as? Map<String, Any>)
                        HistoryItem(
                            id = (item["id"] as? Number)?.toInt() ?: 0,
                            videoId = (video?.get("id") as? Number)?.toInt() ?: 0,
                            videoTitle = (video?.get("title") as? String) ?: "未知",
                            playbackPosition = (item["playback_position"] as? Number)?.toInt() ?: 0,
                            isCompleted = (item["is_completed"] as? Boolean) ?: false,
                            watchedAt = (item["watched_at"] as? String),
                            videoDuration = (video?.get("duration") as? Number)?.toInt()
                        )
                    } ?: emptyList()

                    val grouped = items.groupBy { item ->
                        when {
                            item.watchedAt == null -> "更早"
                            item.watchedAt.startsWith(todayPrefix()) -> "今天"
                            item.watchedAt.startsWith(yesterdayPrefix()) -> "昨天"
                            else -> "更早"
                        }
                    }
                    _uiState.update { it.copy(items = items, grouped = grouped, isLoading = false) }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    fun deleteItem(videoId: Int) {
        viewModelScope.launch {
            historyRepository.deleteVideoHistory(videoId)
                .onSuccess { load() }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clearHistory()
                .onSuccess { _uiState.update { it.copy(items = emptyList(), grouped = emptyMap()) } }
        }
    }

    private fun todayPrefix(): String {
        val cal = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    private fun yesterdayPrefix(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }
}