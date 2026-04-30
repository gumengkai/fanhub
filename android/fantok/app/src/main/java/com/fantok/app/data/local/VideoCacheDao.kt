package com.fantok.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoCacheDao {

    @Query("SELECT * FROM video_cache ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoCache>>

    @Query("SELECT * FROM video_cache WHERE isLiked = 1 ORDER BY createdAt DESC")
    fun getLikedVideos(): Flow<List<VideoCache>>

    @Query("SELECT * FROM video_cache WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteVideos(): Flow<List<VideoCache>>

    @Query("SELECT * FROM video_cache WHERE id = :videoId")
    suspend fun getVideoById(videoId: Int): VideoCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoCache>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoCache)

    @Query("UPDATE video_cache SET isLiked = :isLiked WHERE id = :videoId")
    suspend fun updateLikeStatus(videoId: Int, isLiked: Boolean)

    @Query("UPDATE video_cache SET isFavorite = :isFavorite WHERE id = :videoId")
    suspend fun updateFavoriteStatus(videoId: Int, isFavorite: Boolean)

    @Query("DELETE FROM video_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM video_cache")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM video_cache WHERE isLiked = 1")
    suspend fun getLikedCount(): Int

    @Query("SELECT COUNT(*) FROM video_cache WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int
}
