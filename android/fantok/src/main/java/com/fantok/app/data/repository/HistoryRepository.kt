package com.fantok.app.data.repository

import com.fantok.app.data.api.ApiService
import com.fantok.app.data.model.HistoryRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getAllWatchedVideoIds(): Set<Int> = runCatching {
        val response = apiService.getHistory()
        if (response.isSuccessful) {
            val items = response.body()?.get("items") as? List<Map<String, Any>> ?: emptyList()
            items.mapNotNull { (it["video_id"] as? Number)?.toInt() }.toSet()
        } else {
            emptySet()
        }
    }.getOrDefault(emptySet())

    suspend fun saveProgress(videoId: Int, progress: Float) = runCatching {
        // Get video duration first
        val historyResponse = apiService.getVideoHistory(videoId)
        val duration = if (historyResponse.isSuccessful) {
            (historyResponse.body()?.get("duration") as? Number)?.toInt()
        } else null
        
        val playbackPosition = if (duration != null) {
            (progress * duration).toInt()
        } else 0
        
        apiService.updateVideoHistory(
            videoId,
            HistoryRequest(
                playbackPosition = playbackPosition,
                duration = duration,
                isCompleted = if (duration != null && playbackPosition >= duration * 0.9) true else null
            )
        )
    }

    suspend fun getHistory(): Result<Map<String, Any>> = runCatching {
        val response = apiService.getHistory()
        if (response.isSuccessful) {
            response.body() ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    suspend fun getVideoHistory(videoId: Int): Result<Map<String, Any>> = runCatching {
        val response = apiService.getVideoHistory(videoId)
        if (response.isSuccessful) {
            response.body() ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    suspend fun updateProgress(videoId: Int, playbackPosition: Int, duration: Int? = null): Result<Unit> = runCatching {
        apiService.updateVideoHistory(
            videoId,
            HistoryRequest(
                playbackPosition = playbackPosition,
                duration = duration,
                isCompleted = if (duration != null && playbackPosition >= duration * 0.9) true else null
            )
        )
        Unit
    }

    suspend fun deleteVideoHistory(videoId: Int): Result<Unit> = runCatching {
        apiService.deleteVideoHistory(videoId)
        Unit
    }

    suspend fun clearHistory(): Result<Unit> = runCatching {
        apiService.clearHistory()
        Unit
    }

    suspend fun getStats(): Result<Map<String, Any>> = runCatching {
        val response = apiService.getHistoryStats()
        if (response.isSuccessful) {
            response.body() ?: emptyMap()
        } else {
            emptyMap()
        }
    }
}