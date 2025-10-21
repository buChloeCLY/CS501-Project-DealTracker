package com.example.dealtrackerv1.model

data class Product(
    val name: String,
    val color: String,
    val storage: String,
    val currentPrice: Int,
    val originalPrice: Int,
    val imageUrl: String
)