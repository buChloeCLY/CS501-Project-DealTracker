package com.example.dealtracker.domain.model

// 平台价格信息（用于比价）
data class PlatformPrice(
    val platformName: String,   // 平台名称：Amazon, BestBuy 等
    val platformIcon: String,   // 平台图标（drawable名称或URL）
    val price: Double,           // 该平台上的价格
    val link: String? = null
)