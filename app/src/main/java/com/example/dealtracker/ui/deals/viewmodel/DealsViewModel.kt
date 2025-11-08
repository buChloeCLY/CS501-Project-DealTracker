package com.example.dealtracker.ui.deals.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// ✅ 排序字段枚举 - 移除了 Sales
//enum class SortField(val label: String) {
//    Price("Price"),
//    Rating("Rating")
//}
//
//enum class SortOrder { Asc, Desc }

// ✅ 筛选状态
data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseBestBuy: Boolean = true,
    val chooseWalmart: Boolean = true,
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
            Product(
                pid = 1,
                title = "iPhone 16 Pro",
                price = 999.0,
                rating = 4.6f,
                platform = Platform.Amazon,
                freeShipping = true,
                inStock = true,
                information = "Latest flagship with A18 Pro chip",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 2,
                title = "Samsung Galaxy S24 Ultra",
                price = 1199.0,
                rating = 4.4f,
                platform = Platform.BestBuy,
                freeShipping = false,
                inStock = true,
                information = "200MP camera, S Pen included",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 3,
                title = "OnePlus 12",
                price = 799.0,
                rating = 4.2f,
                platform = Platform.Walmart,
                freeShipping = true,
                inStock = true,
                information = "Snapdragon 8 Gen 3",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 4,
                title = "Google Pixel 9 Pro",
                price = 899.0,
                rating = 4.5f,
                platform = Platform.Amazon,
                freeShipping = false,
                inStock = false,
                information = "Best AI camera features",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 5,
                title = "Sony WH-1000XM5",
                price = 329.0,
                rating = 4.7f,
                platform = Platform.BestBuy,
                freeShipping = true,
                inStock = true,
                information = "Industry-leading noise cancellation",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 6,
                title = "AirPods Pro 2",
                price = 249.0,
                rating = 4.6f,
                platform = Platform.Amazon,
                freeShipping = true,
                inStock = true,
                information = "USB-C charging, adaptive audio",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 7,
                title = "Nintendo Switch OLED",
                price = 349.0,
                rating = 4.8f,
                platform = Platform.Walmart,
                freeShipping = false,
                inStock = true,
                information = "7-inch OLED screen",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 8,
                title = "Kindle Paperwhite",
                price = 139.0,
                rating = 4.5f,
                platform = Platform.Amazon,
                freeShipping = true,
                inStock = true,
                information = "Waterproof e-reader",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 9,
                title = "GoPro HERO12 Black",
                price = 399.0,
                rating = 4.4f,
                platform = Platform.BestBuy,
                freeShipping = false,
                inStock = true,
                information = "5.3K video, waterproof to 33ft",
                category = Category.Electronics,
                imageUrl = ""
            ),
            Product(
                pid = 10,
                title = "Logitech MX Master 3S",
                price = 99.0,
                rating = 4.7f,
                platform = Platform.Walmart,
                freeShipping = true,
                inStock = true,
                information = "Ergonomic wireless mouse",
                category = Category.Electronics,
                imageUrl = ""
            )
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
                        (f.chooseBestBuy && it.platform == Platform.BestBuy) ||
                        (f.chooseWalmart && it.platform == Platform.Walmart) }
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
    fun toggleWalmart(checked: Boolean) = _filters.update { it.copy(chooseWalmart = checked) }
    fun setOnlyFreeShipping(v: Boolean) = _filters.update { it.copy(onlyFreeShipping = v) }
    fun setOnlyInStock(v: Boolean)      = _filters.update { it.copy(onlyInStock = v) }
    fun setSortField(field: SortField)   = _sort.update { it.copy(field = field) }
    fun setSortOrder(order: SortOrder)   = _sort.update { it.copy(order = order) }
    fun clearFilters()                   = _filters.update { DealsFilterState() }
}