package com.example.dealtracker.data.remote.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Data class representing a single item in the user's wishlist response.
 */
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

/**
 * API interface for managing user wishlists and price alerts.
 */
interface WishlistApi {

    /**
     * Retrieves the user's wishlist.
     * @param uid User ID.
     */
    @GET("wishlist")
    suspend fun getWishlist(
        @Query("uid") uid: Int
    ): Response<List<WishlistItemResponse>>

    /**
     * Adds or updates a wishlist item.
     * @param body The request body containing item details.
     */
    @POST("wishlist")
    suspend fun upsertWishlist(
        @Body body: WishlistUpsertRequest
    ): Response<WishlistUpsertResponse>

    /**
     * Deletes a wishlist item.
     * @param body A map containing the item ID to delete.
     */
    @HTTP(method = "DELETE", path = "wishlist", hasBody = true)
    suspend fun deleteWishlist(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    /**
     * Retrieves wishlist items that meet the price alert criteria.
     * @param uid User ID.
     */
    @GET("wishlist/alerts")
    suspend fun getAlerts(
        @Query("uid") uid: Int
    ): Response<List<WishlistItemResponse>>

    /**
     * Marks a price alert as "notified" (sent).
     * @param body A map containing the item ID.
     */
    @POST("wishlist/mark-notified")
    suspend fun markNotified(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    /**
     * Marks a price alert as "read" (user clicked notification).
     * @param body A map containing the item ID.
     */
    @POST("wishlist/mark-read")
    suspend fun markRead(
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>
}