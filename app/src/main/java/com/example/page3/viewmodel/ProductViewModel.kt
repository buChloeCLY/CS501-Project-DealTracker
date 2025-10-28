package com.example.page3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.page3.model.HistoryPointDto
import com.example.page3.model.PlatformPrice
import com.example.page3.model.PricePoint
import com.example.page3.model.Product
import com.example.page3.repository.PriceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.page3.model.Platform

data class HistoryUiState(
    val loading: Boolean = true,
    val data: List<HistoryPointDto> = emptyList(),
    val error: Boolean = false
)

class ProductViewModel : ViewModel() {

    private val repository = PriceRepository()

    fun getProduct(): Product = Product(
        pid = 1,
        title = "iPhone 16",
        price = 999.0,
        rating = 4.6f,
        platform = Platform.Amazon,
        freeShipping = true,
        inStock = true,
        // 可选信息（保留兼容）
        color = "White",
        storage = "256G",
        originalPrice = 1999.0,
        imageUrl = ""
    )

    private val _platformPrices = MutableStateFlow<List<PlatformPrice>>(emptyList())
    val platformPrices: StateFlow<List<PlatformPrice>> = _platformPrices

    fun loadPlatformPrices(pid: Int) {
        viewModelScope.launch {
            _platformPrices.value = repository.getPlatformPrices(pid)
        }
    }

    // ✅ 使用 HistoryUiState 而不是裸 List
    private val _priceHistory = MutableStateFlow(HistoryUiState())
    val priceHistory: StateFlow<HistoryUiState> = _priceHistory

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
