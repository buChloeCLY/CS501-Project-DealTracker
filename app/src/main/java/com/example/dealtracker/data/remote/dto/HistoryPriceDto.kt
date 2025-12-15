package com.example.dealtracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistoryPriceDto(
    @SerializedName("date")
    val date: String,   // "09/01"

    @SerializedName("price")
    val price: Double   // 999.99
)