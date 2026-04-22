package com.fanhub.app.data.model

import com.google.gson.annotations.SerializedName

data class Video(
    @SerializedName("id")              val id: Int,
    @SerializedName("title")           val title: String,
    @SerializedName("path")            val path: String,
    @SerializedName("source_id")       val sourceId: Int,
    @SerializedName("file_size")       val fileSize: Long? = null,
    @SerializedName("duration")        val duration: Int? = null,
    @SerializedName("width")           val width: Int? = null,
    @SerializedName("height")          val height: Int? = null,
    @SerializedName("thumbnail_path")  val thumbnailPath: String? = null,
    @SerializedName("is_favorite")     val isFavorite: Boolean = false,
    @SerializedName("is_liked")        val isLiked: Boolean = false,
    @SerializedName("description")     val description: String? = null,
    @SerializedName("view_count")      val viewCount: Int = 0,
    @SerializedName("tags")            val tags: List<Tag> = emptyList(),
    @SerializedName("created_at")      val createdAt: String? = null,
    @SerializedName("updated_at")      val updatedAt: String? = null
) {
    val resolution: String?
        get() = if (width != null && height != null) "${width}x${height}" else null

    val durationFormatted: String
        get() {
            if (duration == null) return "00:00"
            val mins = duration / 60
            val secs = duration % 60
            return "%02d:%02d".format(mins, secs)
        }

    val fileSizeFormatted: String
        get() {
            if (fileSize == null) return ""
            val mb = fileSize / (1024 * 1024)
            val gb = fileSize / (1024 * 1024 * 1024)
            return if (gb >= 1) "${gb}GB" else "${mb}MB"
        }
}

data class Tag(
    @SerializedName("id")         val id: Int,
    @SerializedName("name")       val name: String,
    @SerializedName("color")      val color: String = "#fb7299",
    @SerializedName("video_count") val videoCount: Int = 0
)

data class VideoListResponse(
    @SerializedName("items")       val items: List<Video>,
    @SerializedName("total")       val total: Int,
    @SerializedName("pages")       val pages: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page")    val perPage: Int
)

// VideoResponse is just a type alias for Video
// The API returns Video object directly
typealias VideoResponse = Video

// Helper to parse single video response
fun parseVideoResponse(json: Map<String, Any>): Video {
    return Video(
        id = (json["id"] as? Number)?.toInt() ?: 0,
        title = json["title"] as? String ?: "",
        path = json["path"] as? String ?: "",
        sourceId = (json["source_id"] as? Number)?.toInt() ?: 0,
        fileSize = (json["file_size"] as? Number)?.toLong(),
        duration = (json["duration"] as? Number)?.toInt(),
        width = (json["width"] as? Number)?.toInt(),
        height = (json["height"] as? Number)?.toInt(),
        thumbnailPath = json["thumbnail_path"] as? String,
        isFavorite = json["is_favorite"] as? Boolean ?: false,
        isLiked = json["is_liked"] as? Boolean ?: false,
        description = json["description"] as? String,
        viewCount = (json["view_count"] as? Number)?.toInt() ?: 0,
        tags = (json["tags"] as? List<Map<String, Any>>)?.map { tagMap ->
            Tag(
                id = (tagMap["id"] as? Number)?.toInt() ?: 0,
                name = tagMap["name"] as? String ?: "",
                color = tagMap["color"] as? String ?: "#fb7299",
                videoCount = (tagMap["video_count"] as? Number)?.toInt() ?: 0
            )
        } ?: emptyList(),
        createdAt = json["created_at"] as? String,
        updatedAt = json["updated_at"] as? String
    )
}

data class TagsResponse(
    @SerializedName("items") val items: List<Tag> = emptyList()
)

data class HistoryRequest(
    @SerializedName("playback_position") val playbackPosition: Int,
    @SerializedName("duration")          val duration: Int? = null,
    @SerializedName("is_completed")      val isCompleted: Boolean? = null
)

data class FavoriteRequest(
    @SerializedName("tag_id") val tagId: Int? = null,
    @SerializedName("video_id") val videoId: Int? = null,
    @SerializedName("image_id") val imageId: Int? = null
)

// Legacy Media class for backward compatibility
data class Media(
    val id: Int,
    val title: String,
    val type: String = "video",
    val thumbnailUrl: String? = null,
    val path: String,
    val duration: Int? = null,
    val rating: Float = 0f,
    val playCount: Int = 0,
    val resolution: String? = null,
    val description: String? = null,
    val sourceId: Int? = null,
    val createTime: String? = null,
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val isLiked: Boolean = false
) {
    // Convert from Video to Media
    companion object {
        fun fromVideo(video: Video, serverUrl: String): Media {
            return Media(
                id = video.id,
                title = video.title,
                type = "video",
                thumbnailUrl = video.thumbnailPath?.let { "$serverUrl/api/videos/${video.id}/thumbnail" },
                path = video.path,
                duration = video.duration,
                playCount = video.viewCount,
                resolution = video.resolution,
                description = video.description,
                sourceId = video.sourceId,
                createTime = video.createdAt,
                tags = video.tags,
                isFavorite = video.isFavorite,
                isLiked = video.isLiked
            )
        }
    }

    // Keep fId for backward compatibility with PlayerManager
    val fId: String = id.toString()
}