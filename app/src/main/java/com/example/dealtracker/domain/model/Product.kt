package com.example.dealtracker.domain.model

/**
 * Domain model for a product, supporting separate short/full titles and multiple low-price platforms.
 */
data class Product(
    val pid: Int,
    val title: String,              // Display title (shortened).
    val fullTitle: String? = null,  // Full product title (for detail view).
    val price: Double,              // Current lowest price.
    val rating: Float,              // Rating (e.g., from Amazon).
    val platform: Platform,         // Primary platform with the lowest price.
    val platformList: List<String> = listOf(platform.name),  // List of all platforms sharing the lowest price.
    val freeShipping: Boolean,      // Free shipping status on the lowest-price platform.
    val inStock: Boolean,           // In-stock status on the lowest-price platform.
    val information: String? = null,// Detailed product information.
    val category: Category,         // Product category.
    val imageUrl: String = ""       // Image URL.
) {
    // Convenience property: formatted price string.
    val priceText: String
        get() = "$%.2f".format(price)

    // Convenience property: text indicating the source of the best price, supporting multiple platforms.
    val sourceText: String
        get() = when {
            platformList.size > 1 -> "Best Price from ${platformList.joinToString(" & ")}"
            else -> when (platform) {
                Platform.Amazon -> "Best Price from Amazon"
                Platform.eBay -> "Best Price from BestBuy"
                Platform.Walmart -> "Best Price from Walmart"
            }
        }

    // Convenience property: title to use in detail views (prefers fullTitle).
    val displayTitle: String
        get() = fullTitle?.takeIf { it.isNotBlank() } ?: title
}