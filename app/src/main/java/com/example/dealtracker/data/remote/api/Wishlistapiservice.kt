package com.example.dealtracker.data.remote.api

import com.example.dealtracker.domain.model.*
import retrofit2.http.*

/**
 * 心愿单 API 接口
 */
interface WishlistApiService {

    /**
     * 获取用户的心愿单
     */
    @GET("api/wishlist/{uid}")
    suspend fun getWishlist(@Path("uid") uid: Int): List<WishlistItem>

    /**
     * 添加商品到心愿单
     */
    @POST("api/wishlist")
    suspend fun addToWishlist(@Body request: AddWishlistRequest): WishlistResponse

    /**
     * 更新心愿单项
     */
    @PUT("api/wishlist/{wid}")
    suspend fun updateWishlist(
        @Path("wid") wid: Int,
        @Body request: UpdateWishlistRequest
    ): WishlistResponse

    /**
     * 删除心愿单项
     */
    @DELETE("api/wishlist/{wid}")
    suspend fun removeFromWishlist(@Path("wid") wid: Int): WishlistResponse

    /**
     * 检查商品是否在心愿单中
     */
    @GET("api/wishlist/check/{uid}/{pid}")
    suspend fun checkWishlist(
        @Path("uid") uid: Int,
        @Path("pid") pid: Int
    ): CheckWishlistResponse

    /**
     * 获取价格提醒
     */
    @GET("api/wishlist/alerts/{uid}")
    suspend fun getAlerts(@Path("uid") uid: Int): AlertsResponse

    /**
     * 获取心愿单统计
     */
    @GET("api/wishlist/stats/{uid}")
    suspend fun getStats(@Path("uid") uid: Int): WishlistStats
}