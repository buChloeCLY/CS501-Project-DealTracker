package com.example.page3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.page3.model.PlatformPrice
import com.example.page3.model.PricePoint
import com.example.page3.model.Product
import com.example.page3.repository.PriceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val repository = PriceRepository()

    // ✅ 当前商品信息还是本地模拟
    fun getProduct(): Product {
        return Product(
            name = "iPhone 16",
            color = "White",
            storage = "256G",
            currentPrice = 999.0,
            originalPrice = 1999.0,
            imageUrl = ""
        )
    }

    // ✅ 历史价格也先保留写死（以后后台也能取）
    fun getPriceHistory(): List<PricePoint> {
        return listOf(
            PricePoint("04-02", 1299.0),
            PricePoint("04-10", 1200.0),
            PricePoint("04-14", 1179.0),
            PricePoint("04-20", 1150.0),
            PricePoint("04-25", 1100.0),
            PricePoint("05-28", 999.0)
        )
    }

    // ✅ 平台价格 —— 改为从数据库/API 获取
    private val _platformPrices = MutableStateFlow<List<PlatformPrice>>(emptyList())
    val platformPrices: StateFlow<List<PlatformPrice>> = _platformPrices

    fun loadPlatformPrices(pid: Int) {
        viewModelScope.launch {
            val result = repository.getPlatformPrices(pid)
            _platformPrices.value = result
        }
    }
}
