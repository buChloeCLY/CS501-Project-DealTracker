package com.example.dealtracker.domain.model

/**
 * 产品领域模型 v2.0
 * 支持完整标题、短标题分离和多平台最低价显示
 */
data class Product(
    val pid: Int,
    val title: String,              // 显示用的标题（short_title）
    val fullTitle: String? = null,  // 🆕 完整标题（用于详情页）
    val price: Double,              // 当前最低价
    val rating: Float,              // 评分（只用 Amazon）
    val platform: Platform,         // 主要最低价平台
    val platformList: List<String> = listOf(platform.name),  // 🆕 所有最低价平台列表
    val freeShipping: Boolean,      // 包邮（最低价平台）
    val inStock: Boolean,           // 有货（最低价平台）
    val information: String? = null,// 详细信息
    val category: Category,         // 分类
    val imageUrl: String = ""       // 图片 URL
) {
    // 便捷属性：价格文本
    val priceText: String
        get() = "$%.2f".format(price)

    // 🆕 便捷属性：来源文本（支持多平台）
    val sourceText: String
        get() = when {
            platformList.size > 1 -> "Best Price from ${platformList.joinToString(" & ")}"
            else -> when (platform) {
                Platform.Amazon -> "Best Price from Amazon"
                Platform.eBay -> "Best Price from BestBuy"
                Platform.Walmart -> "Best Price from Walmart"
            }
        }

    // 便捷属性：用于详情页的标题
    val displayTitle: String
        get() = fullTitle?.takeIf { it.isNotBlank() } ?: title
}