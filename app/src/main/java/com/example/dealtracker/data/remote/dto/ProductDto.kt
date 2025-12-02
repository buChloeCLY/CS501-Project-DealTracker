package com.example.dealtracker.data.remote.dto

import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.google.gson.annotations.SerializedName

/**
 * 产品数据传输对象 v2.0
 * 支持 short_title 和多平台价格
 *
 * 对应后端 /api/products 的 JSON 字段：
 * {
 *   "pid": 2,
 *   "short_title": "...",
 *   "title": "...",
 *   "price": 298,
 *   "rating": 4.5,
 *   "platform": "Amazon" 或 "Amazon, Walmart",
 *   "free_shipping": 1,
 *   "in_stock": 1,
 *   "information": "...",
 *   "category": "Electronics",
 *   "image_url": "..."
 * }
 */
data class ProductDTO(
    val pid: Int,
    val short_title: String?,      // 短标题（和后端字段名一致）
    val title: String,             // 完整标题
    val price: Double,             // 当前最低价
    val rating: Float,             // 评分（只用 Amazon）
    val platform: String,          // 当前最低价平台（可能是逗号分隔）
    @SerializedName("free_shipping")
    val freeShippingRaw: Int,      // 0 / 1 → Boolean 由 toProduct() 负责转换
    @SerializedName("in_stock")
    val inStockRaw: Int,           // 0 / 1 → Boolean 由 toProduct() 负责转换
    val information: String?,      // 详细信息
    val category: String,          // 分类（字符串枚举）
    @SerializedName("image_url")
    val imageUrl: String?          // 图片 URL
) {
    /**
     * 转换为领域模型 Product
     */
    fun toProduct(): Product {
        // 处理 platform 字段（可能是 "Amazon" 或 "Amazon, Walmart"）
        val platformList = platform
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val primaryPlatform = try {
            Platform.valueOf(platformList.firstOrNull() ?: "Amazon")
        } catch (e: IllegalArgumentException) {
            Platform.Amazon // 默认平台
        }

        return Product(
            pid = pid,
            // 优先使用 short_title，如果为空则使用 title 的前 100 字符
            title = short_title?.takeIf { it.isNotBlank() }
                ?: (if (title.length > 100) title.take(100) + "..." else title),
            fullTitle = title,
            price = price,
            rating = rating,
            platform = primaryPlatform,
            platformList = if (platformList.isNotEmpty()) platformList else listOf(primaryPlatform.name),
            freeShipping = (freeShippingRaw == 1),   // ⭐ 0/1 → Boolean
            inStock = (inStockRaw == 1),             // ⭐ 0/1 → Boolean
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
         * 从领域模型创建 DTO（如果以后需要反向发到后端）
         */
        fun fromProduct(product: Product): ProductDTO {
            return ProductDTO(
                pid = product.pid,
                short_title = product.title,
                title = product.fullTitle ?: product.title,
                price = product.price,
                rating = product.rating,
                // 如果有多个平台，用逗号拼起来
                platform = product.platformList.joinToString(", "),
                freeShippingRaw = if (product.freeShipping) 1 else 0,
                inStockRaw = if (product.inStock) 1 else 0,
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
