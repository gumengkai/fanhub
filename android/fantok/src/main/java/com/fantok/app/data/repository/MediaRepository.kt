package com.fantok.app.data.repository

import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.Video
import com.fantok.app.data.model.VideoListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getVideos(
        page: Int = 1,
        perPage: Int = 10,
        sortBy: String = "created_at",
        order: String = "desc",
        favorite: Boolean? = null,
        liked: Boolean? = null,
        watched: Boolean? = null,
        search: String = ""
    ): Result<VideoListResponse> = runCatching {
        apiService.getVideos(
            page = page,
            perPage = perPage,
            sortBy = sortBy,
            order = order,
            favorite = favorite,
            liked = liked,
            watched = watched,
            search = search
        )
    }

    suspend fun getVideo(id: Int): Result<Video> = runCatching {
        apiService.getVideo(id)
    }

    suspend fun toggleFavorite(id: Int): Result<Boolean> = runCatching {
        val response = apiService.toggleFavorite(id)
        response.isSuccessful && (response.body()?.get("is_favorite") as? Boolean ?: false)
    }

    suspend fun toggleLike(id: Int): Result<Boolean> = runCatching {
        val response = apiService.toggleLike(id)
        response.isSuccessful && (response.body()?.get("is_liked") as? Boolean ?: false)
    }

    suspend fun deleteVideo(id: Int): Result<Unit> = runCatching {
        apiService.deleteVideo(id)
        Unit
    }

    suspend fun getRelatedVideos(id: Int): Result<VideoListResponse> = runCatching {
        apiService.getRelatedVideos(id)
    }

    suspend fun updateVideo(id: Int, title: String?, description: String?): Result<Unit> = runCatching {
        val body = mutableMapOf<String, Any>()
        if (title != null) body["title"] = title
        if (description != null) body["description"] = description
        if (body.isNotEmpty()) {
            apiService.updateVideo(id, body)
        }
        Unit
    }

    suspend fun getImages(page: Int = 1, pageSize: Int = 24): Result<ImagesResponse> = runCatching {
        // Return mock empty response for now
        ImagesResponse(emptyList(), 0, 1, page, pageSize)
    }
}

// Image data class and response
data class ImageItem(
    val id: Int = 0,
    val fId: String = "",
    val fTitle: String = "",
    val fThumbnail: String? = null,
    val fPoster: String? = null,
    val path: String = ""
)

data class ImagesResponse(
    val data: List<ImageItem> = emptyList(),
    val total: Int = 0,
    val pages: Int = 1,
    val currentPage: Int = 1,
    val perPage: Int = 24
)