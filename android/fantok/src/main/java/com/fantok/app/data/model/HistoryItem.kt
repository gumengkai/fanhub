package com.fantok.app.data.model

import com.google.gson.annotations.SerializedName

data class HistoryItem(
    @SerializedName("f_id")          val fId: String,
    @SerializedName("f_media_id")    val fMediaId: String,
    @SerializedName("f_progress")    val fProgress: Float = 0f,   // 0~1
    @SerializedName("watched_at")    val watchedAt: String?,
    @SerializedName("f_title")       val fTitle: String?,
    @SerializedName("f_poster")      val fPoster: String?,
    @SerializedName("f_thumbnail")   val fThumbnail: String?,
    @SerializedName("f_type")        val fType: String?,
    @SerializedName("f_duration")    val fDuration: Int?,
    @SerializedName("f_category")    val fCategory: String?
)

data class HistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data")    val data: List<HistoryItem>
)
