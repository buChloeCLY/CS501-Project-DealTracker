package com.example.page3.model

data class PriceDto(
    val id: Long,
    val pid: Long,
    val price: Double,
    val date: String,
    val platform: String,
    val idInPlatform: String,
    val link: String
)
