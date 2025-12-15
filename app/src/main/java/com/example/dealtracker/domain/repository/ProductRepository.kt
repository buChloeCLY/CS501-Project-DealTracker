package com.example.dealtracker.domain.repository

import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.model.SearchResult

/**
 * Interface defining the contract for accessing product data.
 */
interface ProductRepository {

    /**
     * Retrieves a list of all products.
     */
    suspend fun getAllProducts(): Result<List<Product>>

    /**
     * Retrieves a specific product by its ID.
     * @param pid Product ID.
     */
    suspend fun getProductById(pid: Int): Result<Product>

    /**
     * Performs a paginated fuzzy search for products.
     * @param query The search query string.
     * @param page The requested page number (default 1).
     * @param size The number of items per page (default 10).
     * @return A Result containing the search result data.
     */
    suspend fun searchProductsPaged(
        query: String,
        page: Int = 1,
        size: Int = 10
    ): Result<SearchResult>

    /**
     * Retrieves products filtered by a specific platform.
     * @param platform The platform name string.
     */
    suspend fun getProductsByPlatform(platform: String): Result<List<Product>>

    /**
     * Retrieves products filtered within a price range.
     * @param minPrice The minimum price boundary.
     * @param maxPrice The maximum price boundary.
     */
    suspend fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): Result<List<Product>>
}