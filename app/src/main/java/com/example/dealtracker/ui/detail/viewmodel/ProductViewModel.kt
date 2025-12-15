package com.example.dealtracker.ui.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.repository.PriceRepositoryImpl
import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for fetching and managing product price details.
 */
class ProductViewModel : ViewModel() {

    private val repository = PriceRepositoryImpl()

    // State flow for platform-specific prices
    private val _platformPrices = MutableStateFlow<List<PlatformPrice>>(emptyList())
    val platformPrices: StateFlow<List<PlatformPrice>> = _platformPrices

    /**
     * Loads the prices for a product across all platforms.
     * @param pid Product ID.
     */
    fun loadPlatformPrices(pid: Int) {
        viewModelScope.launch {
            _platformPrices.value = repository.getPlatformPrices(pid)
        }
    }

    // State flow for historical price data
    private val _priceHistory = MutableStateFlow(HistoryUiState())
    val priceHistory: StateFlow<HistoryUiState> = _priceHistory

    /**
     * Loads the price history for a product.
     * @param pid Product ID.
     * @param days Number of days of history to fetch (default 7).
     */
    fun loadPriceHistory(pid: Int, days: Int = 7) {
        _priceHistory.value = HistoryUiState(loading = true)
        viewModelScope.launch {
            val data = repository.getPriceHistory(pid, days)
            _priceHistory.value =
                if (data.isEmpty()) HistoryUiState(loading = false, data = emptyList(), error = true)
                else HistoryUiState(loading = false, data = data, error = false)
        }
    }
}