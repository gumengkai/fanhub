package com.fantok.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 视频缓存实体 - 只存储关键信息用于本地筛选和随机播放
 */
@Entity(tableName = "video_cache")
data class VideoCache(
    @PrimaryKey
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
    val createdAt: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
