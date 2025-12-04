package com.example.dealtracker.data.remote.dto

data class SearchResponseDTO(
    val query: String,
    val page: Int,
    val size: Int,
    val total: Int,
    val totalPages: Int,
    val products: List<ProductDTO>
)
