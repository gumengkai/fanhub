package com.fantok.app.data.model

import com.google.gson.annotations.SerializedName

data class Video(
    val id: Int,
    val title: String,
    val path: String,
    @SerializedName("source_id")
    val sourceId: Int,
    @SerializedName("file_size")
    val fileSize: Long? = null,
    val duration: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerializedName("thumbnail_path")
    val thumbnailPath: String? = null,
    @SerializedName("is_favorite")
    val isFavorite: Boolean = false,
    @SerializedName("is_liked")
    val isLiked: Boolean = false,
    val description: String? = null,
    @SerializedName("view_count")
    val viewCount: Int = 0,
    @SerializedName("created_at")
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