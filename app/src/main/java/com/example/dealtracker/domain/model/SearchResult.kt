package com.example.dealtracker.domain.model

data class SearchResult(
    val products: List<Product>,
    val page: Int,
    val totalPages: Int,
    val total: Int
)
