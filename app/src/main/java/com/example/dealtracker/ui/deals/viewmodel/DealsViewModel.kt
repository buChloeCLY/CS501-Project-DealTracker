package com.example.dealtracker.ui.deals.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.repository.ProductRepositoryImpl
import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 排序字段
//enum class SortField(val label: String) {
//    Price("Price"),
//    Rating("Rating")
//}
//
//enum class SortOrder { Asc, Desc }

// 筛选状态
data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseBestBuy: Boolean = true,
    val chooseWalmart: Boolean = true,
    val onlyFreeShipping: Boolean = false,
    val onlyInStock: Boolean = false
)

// 排序状态
data class DealsSortState(
    val field: SortField = SortField.Price,
    val order: SortOrder = SortOrder.Asc
)

// UI 状态
data class DealsUiState(
    val products: List<Product> = emptyList(),
    val filteredSorted: List<Product> = emptyList(),
    val filters: DealsFilterState = DealsFilterState(),
    val sort: DealsSortState = DealsSortState(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DealsViewModel : ViewModel() {

    private val TAG = "DealsViewModel"

    // 使用 Repository
    private val repository: ProductRepository = ProductRepositoryImpl()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _filters = MutableStateFlow(DealsFilterState())
    val filters: StateFlow<DealsFilterState> = _filters

    private val _sort = MutableStateFlow(DealsSortState())
    val sort: StateFlow<DealsSortState> = _sort

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 组合 Flow - 自动计算筛选和排序
    val uiState: StateFlow<DealsUiState> =
        combine(products, filters, sort, isLoading, error) { list, f, s, loading, err ->
            val filtered = list.asSequence()
                .filter { it.price in f.priceMin.toDouble()..f.priceMax.toDouble() }
                .filter {
                    (f.chooseAmazon && it.platform == Platform.Amazon) ||
                            (f.chooseBestBuy && it.platform == Platform.BestBuy) ||
                            (f.chooseWalmart && it.platform == Platform.Walmart)
                }
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
                sort = s,
                isLoading = loading,
                error = err
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DealsUiState(isLoading = true)
        )

    init {
        // 初始化时自动加载产品
        loadProducts()
    }

    /**
     * 从后端 API 加载所有产品
     */
    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d(TAG, "Loading products from backend...")

            repository.getAllProducts()
                .onSuccess { productList ->
                    _products.value = productList
                    Log.d(TAG, "Loaded ${productList.size} products")
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Unknown error"
                    _error.value = errorMsg
                    Log.e(TAG, "Failed to load products: $errorMsg")

                    // 失败时使用备用数据（可选）
                    _products.value = getDummyProducts()
                }

            _isLoading.value = false
        }
    }

    /**
     * 搜索产品
     */
    fun searchProducts(query: String) {
        if (query.isBlank()) {
            loadProducts()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d(TAG, "Searching: $query")

            repository.searchProducts(query)
                .onSuccess { productList ->
                    _products.value = productList
                    Log.d(TAG, "Found ${productList.size} products")
                }
                .onFailure { exception ->
                    _error.value = "Search failed: ${exception.message}"
                    Log.e(TAG, "Search error")
                }

            _isLoading.value = false
        }
    }

    /**
     * 刷新产品列表
     */
    fun refreshProducts() {
        loadProducts()
    }

    /**
     * 备用假数据（API 连接失败时使用）
     */
    private fun getDummyProducts(): List<Product> {
        return listOf(
            Product(
                pid = 999,
                title = "Demo Product - API Not Connected",
                price = 99.0,
                rating = 4.0f,
                platform = Platform.Amazon,
                freeShipping = true,
                inStock = true,
                information = "Please check:\n1. Node.js server is running\n2. Backend URL is correct\n3. Database has data",
                category = Category.Electronics,
                imageUrl = ""
            )
        )
    }

    // ---- 筛选和排序方法 ----
    fun setPrice(min: Float, max: Float) = _filters.update { it.copy(priceMin = min, priceMax = max) }
    fun toggleAmazon(checked: Boolean) = _filters.update { it.copy(chooseAmazon = checked) }
    fun toggleBestBuy(checked: Boolean) = _filters.update { it.copy(chooseBestBuy = checked) }
    fun toggleWalmart(checked: Boolean) = _filters.update { it.copy(chooseWalmart = checked) }
    fun setOnlyFreeShipping(v: Boolean) = _filters.update { it.copy(onlyFreeShipping = v) }
    fun setOnlyInStock(v: Boolean) = _filters.update { it.copy(onlyInStock = v) }
    fun setSortField(field: SortField) = _sort.update { it.copy(field = field) }
    fun setSortOrder(order: SortOrder) = _sort.update { it.copy(order = order) }
    fun clearFilters() = _filters.update { DealsFilterState() }
}