package com.example.dealtracker.data.remote.dto

import com.google.gson.annotations.SerializedName

// 网络价格数据传输对象
data class PriceDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("pid")
    val pid: Long,

    @SerializedName("price")
    val price: Double,

    @SerializedName("date")
    val date: String,

    @SerializedName("platform")
    val platform: String,

    @SerializedName("idInPlatform")
    val idInPlatform: String,

    @SerializedName("link")
    val link: String
)