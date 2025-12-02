package com.example.dealtracker.data.remote.dto

/**
 * 价格 DTO - 用于 /price/:pid 端点
 */
data class PriceDto(
    val id: Int,
    val pid: Int,
    val platform: String,
    val price: Double,
    val free_shipping: Int,
    val in_stock: Int,
    val date: String,
    val link: String?
)

/**
 * 🆕 最低价信息 DTO - 用于 /api/products/:pid/lowest-price 端点
 */
data class LowestPriceDto(
    val lowestPrice: Double,
    val platforms: List<PlatformPriceInfo>,  // 所有最低价平台
    val allPrices: List<PlatformPriceInfo>   // 所有平台价格
)

/**
 * 平台价格详情
 */
data class PlatformPriceInfo(
    val platform: String,
    val price: Double,
    val freeShipping: Boolean,
    val inStock: Boolean,
    val link: String?
)