package com.example.dealtracker.data.remote.dto

import com.google.gson.annotations.SerializedName

// 历史价格数据传输对象
data class HistoryPriceDto(
    @SerializedName("date")
    val date: String,   // "09/01" 格式

    @SerializedName("price")
    val price: Double   // 999.99
)