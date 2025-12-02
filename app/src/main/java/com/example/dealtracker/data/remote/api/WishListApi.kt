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

interface WishlistApi {

    // 对应 server.js 里的 app.get('/api/wishlist', ...)
    // BASE_URL = http://10.0.2.2:8080/api/，这里写 "wishlist"
    @GET("wishlist")
    suspend fun getWishlist(
        @Query("uid") uid: Int
    ): Response<List<WishlistItemResponse>>

    // 对应 app.post('/api/wishlist', ...)
    @POST("wishlist")
    suspend fun upsertWishlist(
        @Body body: WishlistUpsertRequest
    ): Response<Map<String, Any>>

    // 对应 app.delete('/api/wishlist', ...)
    // 这里使用有 body 的 DELETE
    @HTTP(method = "DELETE", path = "wishlist", hasBody = true)
    suspend fun deleteWishlist(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    // 对应 app.get('/api/wishlist/alerts', ...)
    @GET("wishlist/alerts")
    suspend fun getAlerts(
        @Query("uid") uid: Int
    ): Response<List<WishlistItemResponse>>
}
