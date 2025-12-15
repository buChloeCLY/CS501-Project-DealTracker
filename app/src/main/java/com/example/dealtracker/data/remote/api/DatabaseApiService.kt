package com.example.dealtracker.data.remote.api

import com.example.dealtracker.data.remote.dto.ProductDTO
import com.example.dealtracker.data.remote.dto.SearchResponseDTO
import retrofit2.Response
import retrofit2.http.*

/**
 * API interface for accessing product data from the backend server.
 */
interface DatabaseApiService {

    /**
     * Retrieves a list of all products.
     */
    @GET("products")
    suspend fun getAllProducts(): Response<List<ProductDTO>>

    /**
     * Retrieves a specific product by its ID.
     * @param pid Product ID.
     */
    @GET("products/{pid}")
    suspend fun getProductById(@Path("pid") pid: Int): Response<ProductDTO>

    /**
     * Searches for products with optional pagination.
     * @param query Search query string.
     * @param page Page number (default is 1).
     * @param size Number of items per page (default is 10).
     */
    @GET("products/search")
    suspend fun searchProducts(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<SearchResponseDTO>


    /**
     * Filters products by a specific platform.
     * @param platform The name of the platform.
     */
    @GET("products/platform/{platform}")
    suspend fun getByPlatform(@Path("platform") platform: String): Response<List<ProductDTO>>

    /**
     * Filters products within a specified price range.
     * @param minPrice The minimum price for the range.
     * @param maxPrice The maximum price for the range.
     */
    @GET("products/price-range")
    suspend fun getByPriceRange(
        @Query("minPrice") minPrice: Double,
        @Query("maxPrice") maxPrice: Double
    ): Response<List<ProductDTO>>
}