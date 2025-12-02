package com.example.dealtracker.data.remote.api

import com.example.dealtracker.data.remote.dto.ProductDTO
import retrofit2.Response
import retrofit2.http.*

/**
 * 后端 API 接口
 * 连接到你的 Node.js 服务器
 */
interface DatabaseApiService {

    /**
     * 获取所有产品
     */
    @GET("api/products")
    suspend fun getAllProducts(): Response<List<ProductDTO>>

    /**
     * 根据 ID 获取产品
     */
    @GET("api/products/{pid}")
    suspend fun getProductById(@Path("pid") pid: Int): Response<ProductDTO>

    /**
     * 搜索产品（按标题）
     */
    @GET("products/search")
    suspend fun searchProducts(@Query("query") query: String): Response<List<ProductDTO>>

    /**
     * 按平台筛选
     */
    @GET("products/platform/{platform}")
    suspend fun getByPlatform(@Path("platform") platform: String): Response<List<ProductDTO>>

    /**
     * 按价格区间筛选
     */
    @GET("api/products/price-range")
    suspend fun getByPriceRange(
        @Query("minPrice") minPrice: Double,
        @Query("maxPrice") maxPrice: Double
    ): Response<List<ProductDTO>>
}