package com.fantok.app.data.repository

import com.fantok.app.data.local.VideoCache
import com.fantok.app.data.local.VideoCacheDao
import com.fantok.app.data.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCacheRepository @Inject constructor(
    private val videoCacheDao: VideoCacheDao
) {

    fun getAllVideos(): Flow<List<VideoCache>> = videoCacheDao.getAllVideos()

    fun getLikedVideos(): Flow<List<VideoCache>> = videoCacheDao.getLikedVideos()

    fun getFavoriteVideos(): Flow<List<VideoCache>> = videoCacheDao.getFavoriteVideos()

    suspend fun getVideoById(videoId: Int): VideoCache? = videoCacheDao.getVideoById(videoId)

    suspend fun saveVideos(videos: List<Video>) {
        val cachedVideos = videos.map { it.toVideoCache() }
        videoCacheDao.insertVideos(cachedVideos)
    }

    suspend fun updateLikeStatus(videoId: Int, isLiked: Boolean) {
        videoCacheDao.updateLikeStatus(videoId, isLiked)
    }

    suspend fun updateFavoriteStatus(videoId: Int, isFavorite: Boolean) {
        videoCacheDao.updateFavoriteStatus(videoId, isFavorite)
    }

    suspend fun clearCache() {
        videoCacheDao.clearAll()
    }

    suspend fun getCount(): Int = videoCacheDao.getCount()

    suspend fun getLikedCount(): Int = videoCacheDao.getLikedCount()

    suspend fun getFavoriteCount(): Int = videoCacheDao.getFavoriteCount()

    suspend fun isCacheEmpty(): Boolean = getCount() == 0

    /**
     * 获取本地缓存的视频列表（用于筛选和随机播放）
     */
    suspend fun getCachedVideos(filterType: String = "all"): List<Video> {
        return when (filterType) {
            "liked" -> videoCacheDao.getLikedVideos().first()
            "favorite" -> videoCacheDao.getFavoriteVideos().first()
            else -> videoCacheDao.getAllVideos().first()
        }.map { it.toVideo() }
    }
}

/**
 * Video -> VideoCache
 */
fun Video.toVideoCache(): VideoCache {
    return VideoCache(
        id = id,
        title = title,
        path = path,
        sourceId = sourceId,
        fileSize = fileSize,
        duration = duration,
        width = width,
        height = height,
        thumbnailPath = thumbnailPath,
        isFavorite = isFavorite,
        isLiked = isLiked,
        description = description,
        viewCount = viewCount,
        createdAt = createdAt
    )
}

/**
 * VideoCache -> Video
 */
fun VideoCache.toVideo(): Video {
    return Video(
        id = id,
        title = title,
        path = path,
        sourceId = sourceId,
        fileSize = fileSize,
        duration = duration,
        width = width,
        height = height,
        thumbnailPath = thumbnailPath,
        isFavorite = isFavorite,
        isLiked = isLiked,
        description = description,
        viewCount = viewCount,
        createdAt = createdAt
    )
}
