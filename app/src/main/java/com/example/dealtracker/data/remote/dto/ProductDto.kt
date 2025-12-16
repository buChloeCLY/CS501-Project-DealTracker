package com.example.dealtracker.data.remote.dto

import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) for product information from the API.
 */
data class ProductDTO(
    val pid: Int,
    val short_title: String?,      // Shortened title.
    val title: String,             // Full product title.
    val price: Double,             // Current lowest price.
    val rating: Float,             // Product rating.
    val platform: String,          // Platform(s) providing the lowest price (comma-separated).
    @SerializedName("free_shipping")
    val freeShippingRaw: Int,      // Raw int (0/1) for free shipping status.
    @SerializedName("in_stock")
    val inStockRaw: Int,           // Raw int (0/1) for in-stock status.
    val information: String?,      // Detailed product information.
    val category: String,          // Product category name.
    @SerializedName("image_url")
    val imageUrl: String?          // Product image URL.
) {
    /**
     * Converts the ProductDTO to the domain model Product.
     * @return The Product domain model object.
     */
    fun toProduct(): Product {
        // Handle platform field (e.g., "Amazon" or "Amazon, Walmart")
        val platformList = platform
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val primaryPlatform = try {
            Platform.valueOf(platformList.firstOrNull() ?: "Amazon")
        } catch (e: IllegalArgumentException) {
            Platform.Amazon
        }

        return Product(
            pid = pid,
            // Use short_title if available, otherwise truncate full title
            title = short_title?.takeIf { it.isNotBlank() }
                ?: (if (title.length > 100) title.take(100) + "..." else title),
            fullTitle = title,
            price = price,
            rating = rating,
            platform = primaryPlatform,
            platformList = if (platformList.isNotEmpty()) platformList else listOf(primaryPlatform.name),
            freeShipping = (freeShippingRaw == 1),
            inStock = (inStockRaw == 1),
            information = information,
            category = try {
                Category.valueOf(category)
            } catch (e: IllegalArgumentException) {
                Category.Electronics
            },
            imageUrl = imageUrl ?: ""
        )
    }

    companion object {
        /**
         * Creates a ProductDTO from the domain model Product.
         * @param product The Product domain model.
         * @return The ProductDTO object.
         */
        fun fromProduct(product: Product): ProductDTO {
            return ProductDTO(
                pid = product.pid,
                short_title = product.title,
                title = product.fullTitle ?: product.title,
                price = product.price,
                rating = product.rating,
                // Concatenate platform list into a comma-separated string
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
 * Generic API response wrapper.
 * @param success Indicates if the request was successful.
 * @param data The response payload.
 * @param message Optional error or status message.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Request parameters for pagination.
 */
data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "price",
    val sortDirection: String = "ASC"
)

/**
 * Response body for paginated data.
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
 * Filters for product search queries.
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