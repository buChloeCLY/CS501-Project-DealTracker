package com.example.dealtracker.data.remote.api

import retrofit2.Response
import retrofit2.http.*

data class WishlistItemResponse(
    val wid: Int,
    val uid: Int,
    val pid: Int,
    val target_price: Double?,
    val alert_enabled: Int,
    val alert_status: Int,
    val last_alert_time: String?,
    val notes: String?,
    val priority: Int?,
    val short_title: String?,
    val title: String?,
    val category: String?,
    val image_url: String?,
    val rating: Float?,
    val current_price: Double?
)

data class WishlistUpsertRequest(
    val uid: Int,
    val pid: Int,
    val target_price: Double?,
    val alert_enabled: Boolean = true,
    val notes: String? = null,
    val priority: Int? = 2
)

data class WishlistUpsertResponse(
    val success: Boolean,
    val priceReached: Boolean? = null,
    val currentPrice: Double? = null
)

interface WishlistApi {

    // 获取用户 wishlist
    @GET("wishlist")
    suspend fun getWishlist(
        @Query("uid") uid: Int
    ): Response<List<WishlistItemResponse>>

    // 添加/更新 wishlist 项
    @POST("wishlist")
    suspend fun upsertWishlist(
        @Body body: WishlistUpsertRequest
    ): Response<WishlistUpsertResponse>

    // 删除 wishlist 项
    @HTTP(method = "DELETE", path = "wishlist", hasBody = true)
    suspend fun deleteWishlist(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    // 获取需要推送的提醒
    @GET("wishlist/alerts")
    suspend fun getAlerts(
        @Query("uid") uid: Int
    ): Response<List<WishlistItemResponse>>

    // ⭐ 标记为"已推送"
    @POST("wishlist/mark-notified")
    suspend fun markNotified(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    // ⭐ 标记为"已读"（点击通知后调用）
    @POST("wishlist/mark-read")
    suspend fun markRead(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>
}