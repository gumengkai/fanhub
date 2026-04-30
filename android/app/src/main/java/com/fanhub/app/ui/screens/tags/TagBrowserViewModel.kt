package com.fanhub.app.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanhub.app.data.model.Tag
import com.fanhub.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagBrowserUiState(
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TagBrowserViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagBrowserUiState(isLoading = false))
    val uiState = _uiState.asStateFlow()

    private var isInitialized = false

    /**
     * 延迟初始化，在界面准备好后再加载数据
     */
    fun initialize() {
        if (!isInitialized) {
            isInitialized = true
            loadTags()
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            mediaRepository.getTags()
                .onSuccess { tags -> _uiState.update { it.copy(tags = tags, isLoading = false) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}
