package com.fantok.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantok.app.data.model.Video
import com.fantok.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Video> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow.debounce(300).collectLatest { q ->
                if (q.isBlank()) {
                    _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                    return@collectLatest
                }
                _uiState.update { it.copy(isLoading = true) }
                mediaRepository.getVideos(search = q, perPage = 30)
                    .onSuccess { resp -> _uiState.update { it.copy(results = resp.items, isLoading = false) } }
                    .onFailure { _uiState.update { it.copy(isLoading = false) } }
            }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        queryFlow.value = q
    }
}