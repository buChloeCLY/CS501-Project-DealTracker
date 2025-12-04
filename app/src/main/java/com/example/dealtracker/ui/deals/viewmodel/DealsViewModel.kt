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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- 筛选状态 ----------------
data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseBestBuy: Boolean = true,
    val chooseWalmart: Boolean = true,
    val onlyFreeShipping: Boolean = false,
    val onlyInStock: Boolean = false
)

// ---------------- 排序状态 ----------------
data class DealsSortState(
    val field: SortField = SortField.Price,
    val order: SortOrder = SortOrder.Asc
)

// ---------------- UI 状态 ----------------
data class DealsUiState(
    val products: List<Product> = emptyList(),       // 当前页原始数据
    val filteredSorted: List<Product> = emptyList(), // 过滤 + 排序后的列表
    val filters: DealsFilterState = DealsFilterState(),
    val sort: DealsSortState = DealsSortState(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // 分页 & 搜索
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

class DealsViewModel : ViewModel() {

    private val TAG = "DealsViewModel"

    private val repository: ProductRepository = ProductRepositoryImpl()

    // 唯一的状态源
    private val _uiState = MutableStateFlow(DealsUiState(isLoading = true))
    val uiState: StateFlow<DealsUiState> = _uiState

    init {
        // 默认加载全部商品
        loadProducts()
    }

    // ---------------- 工具：根据 products / filters / sort 重新计算 filteredSorted ----------------
    private fun recompute(state: DealsUiState): DealsUiState {
        val f = state.filters
        val s = state.sort

        val filtered = state.products.asSequence()
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

        return state.copy(filteredSorted = sorted)
    }

    // ---------------- 加载全部产品（非搜索） ----------------
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getAllProducts()
                .onSuccess { list ->
                    _uiState.update { old ->
                        recompute(
                            old.copy(
                                products = list,
                                isLoading = false,
                                error = null,
                                searchQuery = "",
                                currentPage = 1,
                                totalPages = 1
                            )
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load products", e)
                    _uiState.update { old ->
                        recompute(
                            old.copy(
                                products = getDummyProducts(),
                                isLoading = false,
                                error = e.message ?: "Unknown error",
                                searchQuery = "",
                                currentPage = 1,
                                totalPages = 1
                            )
                        )
                    }
                }
        }
    }

    // ---------------- 主页传入搜索词时调用 ----------------
    fun applySearch(query: String?) {
        if (query.isNullOrBlank()) {
            // 空搜索 -> 回到全部产品
            loadProducts()
            return
        }

        _uiState.update {
            it.copy(
                searchQuery = query,
                currentPage = 1
            )
        }
        searchPaged()
    }

    // ---------------- 分页搜索 ----------------
    fun searchPaged() {
        val current = _uiState.value
        val q = current.searchQuery
        val page = current.currentPage

        if (q.isBlank()) {
            loadProducts()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.searchProductsPaged(q, page, 10)
                .onSuccess { result ->
                    _uiState.update { old ->
                        recompute(
                            old.copy(
                                products = result.products,
                                isLoading = false,
                                error = null,
                                currentPage = result.page,
                                totalPages = result.totalPages
                            )
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Search failed", e)
                    _uiState.update { old ->
                        old.copy(
                            isLoading = false,
                            error = "Search failed: ${e.message}"
                        )
                    }
                }
        }
    }

    // ---------------- 翻页 ----------------
    fun loadNextPage() {
        val state = _uiState.value
        if (state.currentPage < state.totalPages && state.searchQuery.isNotBlank()) {
            _uiState.update {
                it.copy(currentPage = it.currentPage + 1)
            }
            searchPaged()
        }
    }

    fun loadPrevPage() {
        val state = _uiState.value
        if (state.currentPage > 1 && state.searchQuery.isNotBlank()) {
            _uiState.update {
                it.copy(currentPage = it.currentPage - 1)
            }
            searchPaged()
        }
    }

    // ---------------- 下拉刷新按钮 ----------------
    fun refreshProducts() {
        val q = _uiState.value.searchQuery
        if (q.isNotBlank()) {
            searchPaged()
        } else {
            loadProducts()
        }
    }

    // ---------------- 假数据 ----------------
    private fun getDummyProducts(): List<Product> {
        return listOf(
            Product(
                pid = 999,
                title = "Demo Product - API Not Connected",
                fullTitle = "Demo Product - API Not Connected",
                price = 99.0,
                rating = 4.0f,
                platform = Platform.Amazon,
                platformList = listOf("Amazon"),
                freeShipping = true,
                inStock = true,
                information = "Please check:\n1. Node.js server is running\n2. Backend URL is correct\n3. Database has data",
                category = Category.Electronics,
                imageUrl = ""
            )
        )
    }

    // ---------------- 筛选 / 排序 API ----------------
    fun setPrice(min: Float, max: Float) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(priceMin = min, priceMax = max)))
        }
    }

    fun toggleAmazon(checked: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(chooseAmazon = checked)))
        }
    }

    fun toggleBestBuy(checked: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(chooseBestBuy = checked)))
        }
    }

    fun toggleWalmart(checked: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(chooseWalmart = checked)))
        }
    }

    fun setOnlyFreeShipping(v: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(onlyFreeShipping = v)))
        }
    }

    fun setOnlyInStock(v: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(onlyInStock = v)))
        }
    }

    fun setSortField(field: SortField) {
        _uiState.update { old ->
            recompute(old.copy(sort = old.sort.copy(field = field)))
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { old ->
            recompute(old.copy(sort = old.sort.copy(order = order)))
        }
    }

    fun clearFilters() {
        _uiState.update { old ->
            recompute(old.copy(filters = DealsFilterState()))
        }
    }
}
