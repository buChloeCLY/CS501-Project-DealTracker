package com.example.dealtracker.data.remote.dto

import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product

/**
 * 产品数据传输对象
 * 用于API响应和数据库映射
 */
data class ProductDTO(
    val pid: Int,
    val title: String,
    val price: Double,
    val rating: Float,
    val platform: String,        // API返回字符串格式
    val freeShipping: Boolean,
    val inStock: Boolean,
    val information: String?,
    val category: String,         // API返回字符串格式
    val imageUrl: String?
) {
    /**
     * 转换为领域模型
     */
    fun toProduct(): Product {
        return Product(
            pid = pid,
            title = title,
            price = price,
            rating = rating,
            platform = try {
                Platform.valueOf(platform)
            } catch (e: IllegalArgumentException) {
                Platform.Amazon // 默认平台
            },
            freeShipping = freeShipping,
            inStock = inStock,
            information = information,
            category = try {
                Category.valueOf(category)
            } catch (e: IllegalArgumentException) {
                Category.Electronics // 默认类别
            },
            imageUrl = imageUrl ?: ""
        )
    }

    companion object {
        /**
         * 从领域模型创建DTO
         */
        fun fromProduct(product: Product): ProductDTO {
            return ProductDTO(
                pid = product.pid,
                title = product.title,
                price = product.price,
                rating = product.rating,
                platform = product.platform.name,
                freeShipping = product.freeShipping,
                inStock = product.inStock,
                information = product.information,
                category = product.category.name,
                imageUrl = product.imageUrl
            )
        }
    }
}

/**
 * API响应包装类
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 分页请求参数
 */
data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "price",
    val sortDirection: String = "ASC"
)

/**
 * 分页响应数据
 */
data class PageResponse<T>(
    val content: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * 产品过滤条件
 */
data class ProductFilter(
    val categories: List<String>? = null,
    val platforms: List<String>? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minRating: Float? = null,
    val freeShippingOnly: Boolean = false,
    val inStockOnly: Boolean = false
)