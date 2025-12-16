package com.example.dealtracker.data.remote.api

import com.example.dealtracker.domain.model.History
import com.google.gson.annotations.SerializedName

data class HistoryDto(
    val hid: Int,
    val uid: Int,
    val pid: Int,
    @SerializedName("product_title")
    val productTitle: String,
    @SerializedName("product_image")
    val productImage: String?,
    @SerializedName("product_price")
    val productPrice: Double,
    @SerializedName("product_platform")
    val productPlatform: String,
    @SerializedName("viewed_at")
    val viewedAt: String
) {
    fun toHistory(): History {
        return History(
            hid = hid,
            uid = uid,
            pid = pid,
            productTitle = productTitle,
            productImage = productImage,
            productPrice = productPrice,
            productPlatform = productPlatform,
            viewedAt = viewedAt
        )
    }
}

data class AddHistoryRequest(
    val uid: Int,
    val pid: Int
)

data class AddHistoryResponse(
    val success: Boolean,
    val message: String?,
    val hid: Int?
)