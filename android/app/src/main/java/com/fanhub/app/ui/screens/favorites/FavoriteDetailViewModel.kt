package com.fanhub.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanhub.app.data.model.FavoriteDetail
import com.fanhub.app.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteDetailUiState(
    val detail: FavoriteDetail? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoriteDetailViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(favId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            favoriteRepository.getFavoriteDetail(favId)
                .onSuccess { detail -> _uiState.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}
