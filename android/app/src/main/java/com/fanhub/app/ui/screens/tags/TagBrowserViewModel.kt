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

    private val _uiState = MutableStateFlow(TagBrowserUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.getTags()
                .onSuccess { tags -> _uiState.update { it.copy(tags = tags, isLoading = false) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}
