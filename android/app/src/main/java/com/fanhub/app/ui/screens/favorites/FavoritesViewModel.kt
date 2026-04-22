package com.fanhub.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanhub.app.data.model.Favorite
import com.fanhub.app.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            favoriteRepository.getFavorites()
                .onSuccess { data ->
                    val favoritesList = (data["favorites"] as? List<Map<String, Any>>)?.map { favMap ->
                        Favorite(
                            id = (favMap["id"] as? Number)?.toInt() ?: 0,
                            fId = favMap["f_id"] as? String,
                            fName = favMap["f_name"] as? String,
                            fDescription = favMap["f_description"] as? String,
                            fCreateTime = favMap["f_create_time"] as? String,
                            count = (favMap["count"] as? Number)?.toInt() ?: 0,
                            previews = null
                        )
                    } ?: emptyList()
                    _uiState.update { it.copy(favorites = favoritesList, isLoading = false) }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}