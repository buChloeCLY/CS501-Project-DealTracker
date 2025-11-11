package com.example.dealtracker.domain.model

// 平台枚举（原先在 DealsScreen 内部的 Platform 挪到 model 层）
//enum class Platform { Amazon, BestBuy, Walmart }
//enum class Category { Electronics, Beauty, Home, Food, Fashion, Sports,  }

// 统一、精简且覆盖 UI 所需字段
data class Product(
    val pid: Int,
    val title: String,          // 统一用 title（替代原来的 name）
    val price: Double,          // 统一用 price（替代原来的 currentPrice）
    val rating: Float,          // 评分
    val platform: Platform,     // 最低价平台即“来源”
    val freeShipping: Boolean,  // 包邮
    val inStock: Boolean,       // 有货
    // ↓ 以下为可选信息，保留兼容之前结构（需要就填，不需要就留空）
    val information: String? = null,
    val category: Category,
    val imageUrl: String = ""
){
    // 便捷属性
    val priceText: String get() = "$" + "%.2f".format(price)

    // 动态生成来源信息
    val sourceText: String get() = when (platform) {
        Platform.Amazon -> "Best Price from Amazon"
        Platform.BestBuy -> "Best Price from BestBuy"
        Platform.Walmart -> "Best Price from Walmart"
    }
}
