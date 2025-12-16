package com.example.dealtracker.data.remote.dto

/**
 * price DTO - for /price/:pid
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
 * lowestPrice DTO - for /api/products/:pid/lowest-price
 */
data class LowestPriceDto(
    val lowestPrice: Double,
    val platforms: List<PlatformPriceInfo>,
    val allPrices: List<PlatformPriceInfo>
)

data class PlatformPriceInfo(
    val platform: String,
    val price: Double,
    val freeShipping: Boolean,
    val inStock: Boolean,
    val link: String?
)