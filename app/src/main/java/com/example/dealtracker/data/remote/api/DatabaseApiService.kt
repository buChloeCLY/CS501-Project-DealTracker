package com.example.dealtracker.data.remote.api

import com.example.dealtracker.data.remote.dto.ProductDTO
import com.example.dealtracker.data.remote.dto.SearchResponseDTO
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
    @GET("products")
    suspend fun getAllProducts(): Response<List<ProductDTO>>

    /**
     * 根据 ID 获取产品
     */
    @GET("products/{pid}")
    suspend fun getProductById(@Path("pid") pid: Int): Response<ProductDTO>

    /**
     * 搜索产品（模糊搜索 + 分页）
     */
    @GET("products/search")
    suspend fun searchProducts(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<SearchResponseDTO>


    /**
     * 按平台筛选
     */
    @GET("products/platform/{platform}")
    suspend fun getByPlatform(@Path("platform") platform: String): Response<List<ProductDTO>>

    /**
     * 按价格区间筛选
     */
    @GET("products/price-range")
    suspend fun getByPriceRange(
        @Query("minPrice") minPrice: Double,
        @Query("maxPrice") maxPrice: Double
    ): Response<List<ProductDTO>>
}