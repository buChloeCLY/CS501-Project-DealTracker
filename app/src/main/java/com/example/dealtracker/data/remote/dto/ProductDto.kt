package com.example.dealtracker.data.remote.dto

import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product

/**
 * 产品数据传输对象 v2.0
 * 支持 short_title 和多平台价格
 */
data class ProductDTO(
    val pid: Int,
    val short_title: String?,      // 🆕 短标题（关键词提取后）
    val title: String,              // 完整标题
    val price: Double,              // 当前最低价
    val rating: Float,              // 评分（只用 Amazon）
    val platform: String,           // 当前最低价平台
    val freeShipping: Boolean,      // 包邮（最低价平台的）
    val inStock: Boolean,           // 有货（最低价平台的）
    val information: String?,       // 详细信息
    val category: String,           // 分类
    val imageUrl: String?           // 图片 URL
) {
    /**
     * 转换为领域模型
     */
    fun toProduct(): Product {
        // 处理 platform 字段（可能是 "Amazon" 或 "Amazon, Walmart"）
        val platformList = platform.split(",").map { it.trim() }
        val primaryPlatform = try {
            Platform.valueOf(platformList.first())
        } catch (e: IllegalArgumentException) {
            Platform.Amazon // 默认平台
        }

        return Product(
            pid = pid,
            // 优先使用 short_title，如果为空则使用 title 的前 100 字符
            title = short_title?.takeIf { it.isNotBlank() }
                ?: title.take(100) + if (title.length > 100) "..." else "",
            fullTitle = title,  // 🆕 保留完整标题
            price = price,
            rating = rating,
            platform = primaryPlatform,
            platformList = platformList,  // 🆕 所有最低价平台列表
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
         * 从领域模型创建 DTO
         */
        fun fromProduct(product: Product): ProductDTO {
            return ProductDTO(
                pid = product.pid,
                short_title = product.title,
                title = product.fullTitle ?: product.title,
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
 * API 响应包装类
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