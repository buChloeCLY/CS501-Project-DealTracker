package com.example.dealtracker.ui.detail.viewmodel

import com.example.dealtracker.domain.model.PricePoint

/**
 * UI State for the price history chart data.
 * @param loading Indicates if data is currently being fetched.
 * @param data The list of historical price points.
 * @param error Indicates if an error occurred during fetching.
 */
data class HistoryUiState(
    val loading: Boolean = true,
    val data: List<PricePoint> = emptyList(),
    val error: Boolean = false
)