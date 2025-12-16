package com.example.dealtracker.domain.model

// price points for history chart
data class PricePoint(
    val date: String,    // Date format: "MM-dd"，如 "04-02"
    val price: Double    // The price for this date, such as 1299.0
)