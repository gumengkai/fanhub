package com.fantok.app.data.model

import com.google.gson.annotations.SerializedName

data class Video(
    val id: Int,
    val title: String,
    val path: String,
    val sourceId: Int,
    val fileSize: Long? = null,
    val duration: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false,
    val isLiked: Boolean = false,
    val description: String? = null,
    val viewCount: Int = 0,
    val createdAt: String? = null
) {
    fun getThumbnailUrl(baseUrl: String): String {
        return "$baseUrl/api/douyin/$id/thumbnail"
    }

    fun getStreamUrl(baseUrl: String): String {
        return "$baseUrl/api/douyin/$id/stream"
    }
}

data class VideoListResponse(
    val items: List<Video>,
    val total: Int,
    val pages: Int,
    val currentPage: Int,
    val perPage: Int
)

data class DouyinStats(
    val total: Int,
    val liked: Int,
    val favorite: Int,
    val totalSize: Long,
    val totalDuration: Int
)