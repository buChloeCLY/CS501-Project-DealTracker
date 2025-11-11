package com.example.dealtracker.domain.repository

import com.example.dealtracker.domain.model.Product

/**
 * 产品数据仓库接口（领域层）
 */
interface ProductRepository {

    /**
     * 获取所有产品
     */
    suspend fun getAllProducts(): Result<List<Product>>

    /**
     * 根据 ID 获取产品
     */
    suspend fun getProductById(pid: Int): Result<Product>

    /**
     * 搜索产品
     */
    suspend fun searchProducts(query: String): Result<List<Product>>

    /**
     * 按平台筛选
     */
    suspend fun getProductsByPlatform(platform: String): Result<List<Product>>

    /**
     * 按价格区间筛选
     */
    suspend fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): Result<List<Product>>
}