package com.fantok.app.data.model

import com.google.gson.annotations.SerializedName

data class Favorite(
    @SerializedName("id")            val id: Int = 0,
    @SerializedName("f_id")          val fId: String? = null,
    @SerializedName("f_name")        val fName: String? = null,
    @SerializedName("name")          val name: String = "",
    @SerializedName("f_description") val fDescription: String?,
    @SerializedName("f_create_time") val fCreateTime: String?,
    @SerializedName("count")         val count: Int = 0,
    @SerializedName("total_count")   val totalCount: Int = 0,
    @SerializedName("previews")      val previews: List<Media>? = null
)

data class FavoritesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data")    val data: List<Favorite>
)

data class FavoriteDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data")    val data: FavoriteDetail
)

data class FavoriteDetail(
    @SerializedName("id")            val id: Int = 0,
    @SerializedName("f_id")          val fId: String? = null,
    @SerializedName("f_name")        val fName: String? = null,
    @SerializedName("name")          val name: String = "",
    @SerializedName("f_description") val fDescription: String?,
    @SerializedName("items")         val items: List<Media> = emptyList(),
    @SerializedName("total_count")   val totalCount: Int = 0
)
