package com.example.dealtracker.ui.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.repository.PriceRepositoryImpl
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val repository = PriceRepositoryImpl()

    // 模拟获取产品信息（后续可从API获取）
    fun getProduct(): Product = Product(
        pid = 1,
        title = "iPhone 16",
        price = 999.0,
        rating = 4.6f,
        platform = Platform.Amazon,
        freeShipping = true,
        inStock = true,
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

    // 使用 PricePoint 而不是 HistoryPriceDto
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