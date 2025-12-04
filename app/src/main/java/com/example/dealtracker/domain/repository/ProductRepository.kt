package com.example.dealtracker.domain.repository

import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.model.SearchResult

interface ProductRepository {

    suspend fun getAllProducts(): Result<List<Product>>

    suspend fun getProductById(pid: Int): Result<Product>

    /**
     * 分页模糊搜索
     */
    suspend fun searchProductsPaged(
        query: String,
        page: Int = 1,
        size: Int = 10
    ): Result<SearchResult>

    suspend fun getProductsByPlatform(platform: String): Result<List<Product>>

    suspend fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): Result<List<Product>>
}
