package com.example.dealtracker.domain.model

data class PlatformPrice(
    val platformName: String,
    val platformIcon: String,
    val price: Double,
    val link: String? = null
)