package com.example.dealtracker.ui.detail.viewmodel

import com.example.dealtracker.domain.model.PricePoint

// 使用 PricePoint 而不是 HistoryPriceDto
data class HistoryUiState(
    val loading: Boolean = true,
    val data: List<PricePoint> = emptyList(),
    val error: Boolean = false
)