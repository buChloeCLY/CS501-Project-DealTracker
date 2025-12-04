package com.example.dealtracker.domain.model

data class History(
    val hid: Int,
    val uid: Int,
    val pid: Int,
    val productTitle: String,
    val productImage: String?,
    val productPrice: Double,
    val productPlatform: String,
    val viewedAt: String
)