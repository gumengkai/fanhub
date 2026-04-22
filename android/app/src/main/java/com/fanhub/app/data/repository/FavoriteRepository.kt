package com.fanhub.app.data.repository

import com.fanhub.app.data.api.ApiService
import com.fanhub.app.data.model.FavoriteDetail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getFavorites(): Result<Map<String, Any>> = runCatching {
        val response = apiService.getFavorites()
        if (response.isSuccessful) {
            response.body() ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    suspend fun getStats(): Result<Map<String, Any>> = runCatching {
        val response = apiService.getFavoritesStats()
        if (response.isSuccessful) {
            response.body() ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    suspend fun toggleVideoFavorite(videoId: Int): Result<Boolean> = runCatching {
        val response = apiService.toggleFavorite(videoId)
        response.isSuccessful && (response.body()?.get("is_favorite") as? Boolean ?: false)
    }

    suspend fun toggleImageFavorite(imageId: Int): Result<Boolean> = runCatching {
        val response = apiService.toggleImageFavorite(imageId)
        response.isSuccessful && (response.body()?.get("is_favorite") as? Boolean ?: false)
    }

    suspend fun getFavoriteDetail(favId: Int): Result<FavoriteDetail> = runCatching {
        // Create a mock favorite detail for now
        FavoriteDetail(
            id = favId,
            name = "收藏夹 $favId",
            items = emptyList(),
            totalCount = 0,
            fId = favId.toString(),
            fName = "收藏夹 $favId",
            fDescription = null
        )
    }
}