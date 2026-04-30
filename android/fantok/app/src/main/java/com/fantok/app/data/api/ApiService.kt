package com.fantok.app.data.api

import com.fantok.app.data.model.DouyinStats
import com.fantok.app.data.model.VideoListResponse
import com.fantok.app.data.model.Video
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/douyin")
    suspend fun getDouyinVideos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("liked") liked: Boolean? = null,
        @Query("favorite") favorite: Boolean? = null
    ): VideoListResponse

    @GET("api/douyin/{id}")
    suspend fun getDouyinVideo(@Path("id") id: Int): Video

    @POST("api/douyin/{id}/like")
    suspend fun toggleLike(@Path("id") id: Int): Response<Map<String, Any>>

    @POST("api/douyin/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: Int): Response<Map<String, Any>>

    @DELETE("api/douyin/{id}")
    suspend fun deleteVideo(@Path("id") id: Int): Response<Unit>

    @GET("api/douyin/stats")
    suspend fun getDouyinStats(): DouyinStats

    @GET("api/health")
    suspend fun healthCheck(): Response<Map<String, String>>
}