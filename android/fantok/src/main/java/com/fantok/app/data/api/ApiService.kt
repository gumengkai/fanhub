package com.fantok.app.data.api

import com.fantok.app.data.model.FavoriteRequest
import com.fantok.app.data.model.HistoryRequest
import com.fantok.app.data.model.TagsResponse
import com.fantok.app.data.model.VideoListResponse
import com.fantok.app.data.model.Video
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── Health Check ──────────────────────────────────────────────────────────
    @GET("api/health")
    suspend fun healthCheck(): Response<Map<String, String>>

    // ── Douyin Videos ───────────────────────────────────────────────────────
    @GET("api/douyin")
    suspend fun getVideos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String = "",
        @Query("sort_by") sortBy: String = "created_at",
        @Query("order") order: String = "desc",
        @Query("tag_id") tagId: Int? = null,
        @Query("favorite") favorite: Boolean? = null,
        @Query("liked") liked: Boolean? = null,
        @Query("is_watched") watched: Boolean? = null
    ): VideoListResponse

    @GET("api/douyin/{id}")
    suspend fun getVideo(@Path("id") id: Int): Video

    @GET("api/douyin/{id}/stream")
    suspend fun getVideoStream(@Path("id") id: Int): Response<okhttp3.ResponseBody>

    @PUT("api/douyin/{id}")
    suspend fun updateVideo(
        @Path("id") id: Int,
        @Body body: Map<String, Any>
    ): Video

    @DELETE("api/douyin/{id}")
    suspend fun deleteVideo(@Path("id") id: Int): Response<Unit>

    @POST("api/douyin/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: Int): Response<Map<String, Any>>

    @POST("api/douyin/{id}/like")
    suspend fun toggleLike(@Path("id") id: Int): Response<Map<String, Any>>

    @GET("api/douyin/{id}/thumbnail")
    suspend fun getThumbnail(@Path("id") id: Int): Response<okhttp3.ResponseBody>

    @GET("api/douyin/{id}/tags")
    suspend fun getVideoTags(@Path("id") id: Int): TagsResponse

    @POST("api/douyin/{id}/tags")
    suspend fun addTagToVideo(
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): TagsResponse

    @DELETE("api/douyin/{id}/tags/{tagId}")
    suspend fun removeTagFromVideo(
        @Path("id") id: Int,
        @Path("tagId") tagId: Int
    ): TagsResponse

    @GET("api/douyin/{id}/related")
    suspend fun getRelatedVideos(@Path("id") id: Int): VideoListResponse

    // ── Watch History ───────────────────────────────────────────────────────────
    @GET("api/douyin/{id}/history")
    suspend fun getVideoHistory(@Path("id") id: Int): Response<Map<String, Any>>

    @POST("api/douyin/{id}/history")
    suspend fun updateVideoHistory(
        @Path("id") id: Int,
        @Body body: HistoryRequest
    ): Response<Map<String, Any>>

    // ── Tags ───────────────────────────────────────────────────────────────────
    @GET("api/tags")
    suspend fun getTags(): TagsResponse

    @POST("api/tags")
    suspend fun createTag(@Body body: Map<String, String>): Response<Map<String, Any>>

    @PUT("api/tags/{id}")
    suspend fun updateTag(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @DELETE("api/tags/{id}")
    suspend fun deleteTag(@Path("id") id: Int): Response<Unit>

    // ── Favorites ───────────────────────────────────────────────────────────────
    @GET("api/favorites")
    suspend fun getFavorites(): Response<Map<String, Any>>

    @GET("api/favorites/stats")
    suspend fun getFavoritesStats(): Response<Map<String, Any>>

    // ── Likes ───────────────────────────────────────────────────────────────────
    @GET("api/likes")
    suspend fun getLikes(): Response<Map<String, Any>>

    @GET("api/likes/stats")
    suspend fun getLikesStats(): Response<Map<String, Any>>

    // ── History ───────────────────────────────────────────────────────────────────
    @GET("api/history")
    suspend fun getHistory(): Response<Map<String, Any>>

    @GET("api/history/stats")
    suspend fun getHistoryStats(): Response<Map<String, Any>>

    @DELETE("api/history/video/{videoId}")
    suspend fun deleteVideoHistory(@Path("videoId") videoId: Int): Response<Unit>

    @POST("api/history/clear")
    suspend fun clearHistory(): Response<Unit>

    // ── Sources ───────────────────────────────────────────────────────────────────
    @GET("api/sources")
    suspend fun getSources(): Response<Map<String, Any>>

    @POST("api/sources/{id}/scan")
    suspend fun scanSource(@Path("id") id: Int): Response<Map<String, Any>>
}
