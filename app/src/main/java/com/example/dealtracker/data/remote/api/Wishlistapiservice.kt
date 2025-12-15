package com.example.dealtracker.data.remote.api

import com.example.dealtracker.ui.wishlist.viewmodel.AddWishlistRequest
import com.example.dealtracker.ui.wishlist.viewmodel.AlertsResponse
import com.example.dealtracker.ui.wishlist.viewmodel.CheckWishlistResponse
import com.example.dealtracker.ui.wishlist.viewmodel.UpdateWishlistRequest
import com.example.dealtracker.ui.wishlist.viewmodel.WishlistItem
import com.example.dealtracker.ui.wishlist.viewmodel.WishlistResponse
import com.example.dealtracker.ui.wishlist.viewmodel.WishlistStats
import retrofit2.http.*

/**
 * Wishlist API Interface for managing user's tracked deals.
 */
interface WishlistApiService {

    /**
     * Retrieves the user's wishlist items.
     * @param uid User ID.
     */
    @GET("api/wishlist/{uid}")
    suspend fun getWishlist(@Path("uid") uid: Int): List<WishlistItem>

    /**
     * Adds a product to the user's wishlist.
     * @param request The request body containing product and alert details.
     */
    @POST("api/wishlist")
    suspend fun addToWishlist(@Body request: AddWishlistRequest): WishlistResponse

    /**
     * Updates a specific wishlist item.
     * @param wid Wishlist item ID.
     * @param request The update request body.
     */
    @PUT("api/wishlist/{wid}")
    suspend fun updateWishlist(
        @Path("wid") wid: Int,
        @Body request: UpdateWishlistRequest
    ): WishlistResponse

    /**
     * Removes a product from the user's wishlist.
     * @param wid Wishlist item ID.
     */
    @DELETE("api/wishlist/{wid}")
    suspend fun removeFromWishlist(@Path("wid") wid: Int): WishlistResponse

    /**
     * Checks if a specific product is already in the user's wishlist.
     * @param uid User ID.
     * @param pid Product ID.
     */
    @GET("api/wishlist/check/{uid}/{pid}")
    suspend fun checkWishlist(
        @Path("uid") uid: Int,
        @Path("pid") pid: Int
    ): CheckWishlistResponse

    /**
     * Fetches the user's price alerts.
     * @param uid User ID.
     */
    @GET("api/wishlist/alerts/{uid}")
    suspend fun getAlerts(@Path("uid") uid: Int): AlertsResponse

    /**
     * Retrieves statistics for the user's wishlist.
     * @param uid User ID.
     */
    @GET("api/wishlist/stats/{uid}")
    suspend fun getStats(@Path("uid") uid: Int): WishlistStats
}