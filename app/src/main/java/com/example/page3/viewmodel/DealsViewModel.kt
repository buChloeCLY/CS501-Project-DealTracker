package com.example.page3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.page3.model.Platform
import com.example.page3.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// ✅ 排序字段枚举 - 移除了 Sales
enum class SortField(val label: String) {
    Price("Price"),
    Rating("Rating")
}

enum class SortOrder { Asc, Desc }

// ✅ 筛选状态
data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseBestBuy: Boolean = true,
    val onlyFreeShipping: Boolean = false,
    val onlyInStock: Boolean = false
)

// ✅ 排序状态 - 默认按价格排序
data class DealsSortState(
    val field: SortField = SortField.Price,
    val order: SortOrder = SortOrder.Asc
)

// ✅ UI 状态
data class DealsUiState(
    val products: List<Product> = emptyList(),
    val filteredSorted: List<Product> = emptyList(),
    val filters: DealsFilterState = DealsFilterState(),
    val sort: DealsSortState = DealsSortState()
)

class DealsViewModel : ViewModel() {

    // ✅ 样例数据 - 移除了 sales 和 source
    private val _products = MutableStateFlow(
        listOf(
            Product(1, "iPhone 16 Pro", price = 999.0, originalPrice = 1199.0, imageUrl = "",
                rating = 4.6f, platform = Platform.Amazon, freeShipping = true, inStock = true),

            Product(2, "Samsung Galaxy Ultra", price = 999.0, originalPrice = 1299.0, imageUrl = "",
                rating = 4.4f, platform = Platform.Amazon, freeShipping = false, inStock = true),

            Product(3, "OnePlus 12", price = 799.0, originalPrice = 899.0, imageUrl = "",
                rating = 4.2f, platform = Platform.BestBuy, freeShipping = true, inStock = true),

            Product(4, "Google Pixel 9", price = 899.0, originalPrice = 999.0, imageUrl = "",
                rating = 4.5f, platform = Platform.BestBuy, freeShipping = false, inStock = false),

            Product(5, "Moto X Pro", price = 699.0, originalPrice = 799.0, imageUrl = "",
                rating = 3.9f, platform = Platform.Amazon, freeShipping = true, inStock = true),

            Product(6, "Sony WH-1000XM5", price = 329.0, originalPrice = 399.0, imageUrl = "",
                rating = 4.7f, platform = Platform.BestBuy, freeShipping = true, inStock = true),

            Product(7, "AirPods Pro 2", price = 249.0, originalPrice = 299.0, imageUrl = "",
                rating = 4.6f, platform = Platform.Amazon, freeShipping = true, inStock = true),

            Product(8, "Nintendo Switch OLED", price = 349.0, originalPrice = 349.0, imageUrl = "",
                rating = 4.8f, platform = Platform.BestBuy, freeShipping = false, inStock = true),

            Product(9, "Kindle Paperwhite", price = 139.0, originalPrice = 159.0, imageUrl = "",
                rating = 4.5f, platform = Platform.Amazon, freeShipping = true, inStock = true),

            Product(10, "GoPro HERO12", price = 399.0, originalPrice = 449.0, imageUrl = "",
                rating = 4.4f, platform = Platform.BestBuy, freeShipping = false, inStock = true),

            Product(11, "Logitech MX Master 3S", price = 99.0, originalPrice = 129.0, imageUrl = "",
                rating = 4.7f, platform = Platform.Amazon, freeShipping = true, inStock = true),
        )
    )
    val products: StateFlow<List<Product>> = _products

    private val _filters = MutableStateFlow(DealsFilterState())
    val filters: StateFlow<DealsFilterState> = _filters

    private val _sort = MutableStateFlow(DealsSortState())
    val sort: StateFlow<DealsSortState> = _sort

    // ✅ 组合 Flow - 自动计算筛选和排序后的结果
    val uiState: StateFlow<DealsUiState> =
        combine(products, filters, sort) { list, f, s ->
            val filtered = list.asSequence()
                .filter { it.price in f.priceMin.toDouble()..f.priceMax.toDouble() }
                .filter { (f.chooseAmazon && it.platform == Platform.Amazon) ||
                        (f.chooseBestBuy && it.platform == Platform.BestBuy) }
                .filter { if (f.onlyFreeShipping) it.freeShipping else true }
                .filter { if (f.onlyInStock) it.inStock else true }
                .toList()

            val sorted = when (s.field) {
                SortField.Price  -> filtered.sortedBy { it.price }
                SortField.Rating -> filtered.sortedBy { it.rating }
            }.let { if (s.order == SortOrder.Desc) it.reversed() else it }

            DealsUiState(
                products = list,
                filteredSorted = sorted,
                filters = f,
                sort = s
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DealsUiState(products = _products.value, filteredSorted = _products.value)
        )

    // ---- 更新方法 ----
    fun setPrice(min: Float, max: Float) = _filters.update { it.copy(priceMin = min, priceMax = max) }
    fun toggleAmazon(checked: Boolean)  = _filters.update { it.copy(chooseAmazon = checked) }
    fun toggleBestBuy(checked: Boolean) = _filters.update { it.copy(chooseBestBuy = checked) }
    fun setOnlyFreeShipping(v: Boolean) = _filters.update { it.copy(onlyFreeShipping = v) }
    fun setOnlyInStock(v: Boolean)      = _filters.update { it.copy(onlyInStock = v) }
    fun setSortField(field: SortField)   = _sort.update { it.copy(field = field) }
    fun setSortOrder(order: SortOrder)   = _sort.update { it.copy(order = order) }
    fun clearFilters()                   = _filters.update { DealsFilterState() }
}