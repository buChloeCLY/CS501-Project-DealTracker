package com.example.dealtracker.domain.model

// 统一、精简且覆盖 UI 所需字段的产品模型
data class Product(
    val pid: Int,
    val title: String,          // 产品标题
    val price: Double,          // 当前价格
    val rating: Float,          // 用户评分
    val platform: Platform,     // 平台来源
    val freeShipping: Boolean,  // 是否包邮
    val inStock: Boolean,       // 是否有货
    // 可选信息，保留兼容性
    val color: String? = null,
    val storage: String? = null,
    val originalPrice: Double? = null,
    val imageUrl: String = ""
) {
    // 便捷属性：格式化价格显示
    val priceText: String get() = "$" + "%.2f".format(price)

    // 动态生成来源信息
    val sourceText: String get() = when (platform) {
        Platform.Amazon -> "Best Price from Amazon"
        Platform.BestBuy -> "Best Price from BestBuy"
    }
}