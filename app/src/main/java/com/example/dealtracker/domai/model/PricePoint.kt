package com.example.dealtracker.domain.model

// ✅ 价格点（用于价格走势图表）
data class PricePoint(
    val date: String,    // 日期格式："MM-dd"，如 "04-02"
    val price: Double    // 该日期的价格，如 1299.0
)