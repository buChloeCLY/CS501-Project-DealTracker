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

// ---------------- Filter State ----------------
/**
 * Represents the current filtering criteria for deals.
 */
data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseEBay: Boolean = true,
    val chooseWalmart: Boolean = true,
    val onlyFreeShipping: Boolean = false,
    val onlyInStock: Boolean = false
)

enum class DealsMode { DEFAULT, SEARCH, CATEGORY }

// ---------------- Sort State ----------------
/**
 * Represents the current sorting criteria for deals.
 */
data class DealsSortState(
    val field: SortField = SortField.Price,
    val order: SortOrder = SortOrder.Asc
)

// ---------------- UI State ----------------
/**
 * Represents the entire UI state for the Deals screen.
 */
data class DealsUiState(
    val products: List<Product> = emptyList(),       // Raw data fetched from API (current page or all).
    val filteredSorted: List<Product> = emptyList(), // Filtered and sorted list derived from 'products'.
    val filters: DealsFilterState = DealsFilterState(),
    val sort: DealsSortState = DealsSortState(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // Pagination & Search
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    val mode: DealsMode = DealsMode.DEFAULT,
    val currentCategory: String? = null

)

/**
 * ViewModel for managing product deals data, filtering, sorting, and pagination.
 */
class DealsViewModel : ViewModel() {

    private val TAG = "DealsViewModel"

    private val repository: ProductRepository = ProductRepositoryImpl()

    // The single source of UI state
    private val _uiState = MutableStateFlow(DealsUiState(isLoading = true))
    val uiState: StateFlow<DealsUiState> = _uiState

    // ---------------- Utility: Recompute filteredSorted list ----------------
    /**
     * Applies the current filters and sort order to the raw product list.
     * @param state The current DealsUiState.
     * @return The updated DealsUiState with the new filteredSorted list.
     */
    private fun recompute(state: DealsUiState): DealsUiState {
        val f = state.filters
        val s = state.sort

        val filtered = state.products.asSequence()
            .filter { it.price in f.priceMin.toDouble()..f.priceMax.toDouble() }
            .filter {
                (f.chooseAmazon && it.platform == Platform.Amazon) ||
                        (f.chooseEBay && it.platform == Platform.eBay) ||
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

    // ---------------- Load All Products (Non-Search) ----------------
    /**
     * Loads all available products from the repository.
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, mode = DealsMode.DEFAULT, currentCategory = null) }

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
                                totalPages = 1,
                                mode = DealsMode.DEFAULT,
                                currentCategory = null
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
                                totalPages = 1,
                                mode = DealsMode.DEFAULT,
                                currentCategory = null
                            )
                        )
                    }
                }
        }
    }


    // ---------------- Apply Category Filter (from home screen) ----------------
    /**
     * Loads all products and filters them locally by the given category name.
     * @param categoryName The category name string to filter by.
     */
    fun applyCategory(categoryName: String) {
        val cat = categoryName.trim()
        if (cat.isBlank()) {
            loadProducts()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    searchQuery = "",
                    currentPage = 1,
                    totalPages = 1,
                    mode = DealsMode.CATEGORY,
                    currentCategory = cat
                )
            }

            repository.getAllProducts()
                .onSuccess { list ->
                    val filtered = list.filter { product ->
                        product.category.toString().equals(cat, ignoreCase = true)
                    }
                    _uiState.update { old ->
                        recompute(
                            old.copy(
                                products = filtered,
                                isLoading = false,
                                error = null,
                                mode = DealsMode.CATEGORY,
                                currentCategory = cat
                            )
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load products for category", e)
                    _uiState.update { old ->
                        old.copy(isLoading = false, error = e.message ?: "Unknown error")
                    }
                }
        }
    }



    // ---------------- Apply Search Query (from home screen) ----------------
    /**
     * Initiates a paginated search or reverts to loading all products if the query is blank.
     * @param query The search term.
     */
    fun applySearch(query: String?) {
        val q = query?.trim().orEmpty()
        if (q.isBlank()) {
            loadProducts()
            return
        }

        _uiState.update {
            it.copy(
                searchQuery = q,
                currentPage = 1,
                mode = DealsMode.SEARCH,
                currentCategory = null
            )
        }
        searchPaged()
    }

    // ---------------- Paginated Search ----------------
    /**
     * Executes the paginated product search based on current state (query and page).
     */
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
                                totalPages = result.totalPages,
                                mode = DealsMode.SEARCH,      // keep search
                                currentCategory = null        // clear category
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

    // ---------------- Pagination Controls ----------------
    /**
     * Loads the next page of search results if available.
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (state.mode == DealsMode.SEARCH && state.currentPage < state.totalPages) {
            _uiState.update {
                it.copy(currentPage = it.currentPage + 1)
            }
            searchPaged()
        }
    }

    /**
     * Loads the previous page of search results if available.
     */
    fun loadPrevPage() {
        val state = _uiState.value
        if (state.mode == DealsMode.SEARCH && state.currentPage > 1) {
            _uiState.update {
                it.copy(currentPage = it.currentPage - 1)
            }
            searchPaged()
        }
    }

    // ---------------- Refresh Button ----------------
    /**
     * Refreshes the current list, either by re-running the search or loading all products.
     */
    fun refreshProducts() {
        when (_uiState.value.mode) {
            DealsMode.SEARCH -> {
                // stay in keyword search mode
                searchPaged()
            }
            DealsMode.CATEGORY -> {
                // re-fetch all then filter by category (your requirement)
                val cat = _uiState.value.currentCategory
                if (!cat.isNullOrBlank()) applyCategory(cat) else loadProducts()
            }
            DealsMode.DEFAULT -> {
                // stay in default deals mode
                loadProducts()
            }
        }
    }

    // ---------------- Dummy Data ----------------
    /**
     * Generates a list of dummy products used when the API connection fails.
     */
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

    // ---------------- Filter / Sort API ----------------
    /**
     * Sets the price range filter and recomputes the list.
     * @param min Minimum price.
     * @param max Maximum price.
     */
    fun setPrice(min: Float, max: Float) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(priceMin = min, priceMax = max)))
        }
    }

    /**
     * Toggles the Amazon platform filter.
     * @param checked New checked state.
     */
    fun toggleAmazon(checked: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(chooseAmazon = checked)))
        }
    }

    /**
     * Toggles the eBay platform filter.
     * @param checked New checked state.
     */
    fun toggleEBay(checked: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(chooseEBay = checked)))
        }
    }

    /**
     * Toggles the Walmart platform filter.
     * @param checked New checked state.
     */
    fun toggleWalmart(checked: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(chooseWalmart = checked)))
        }
    }

    /**
     * Toggles the free shipping filter.
     * @param v New filter state.
     */
    fun setOnlyFreeShipping(v: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(onlyFreeShipping = v)))
        }
    }

    /**
     * Toggles the in-stock filter.
     * @param v New filter state.
     */
    fun setOnlyInStock(v: Boolean) {
        _uiState.update { old ->
            recompute(old.copy(filters = old.filters.copy(onlyInStock = v)))
        }
    }

    /**
     * Sets the field used for sorting (Price or Rating).
     * @param field The new sort field.
     */
    fun setSortField(field: SortField) {
        _uiState.update { old ->
            recompute(old.copy(sort = old.sort.copy(field = field)))
        }
    }

    /**
     * Sets the sort order (Ascending or Descending).
     * @param order The new sort order.
     */
    fun setSortOrder(order: SortOrder) {
        _uiState.update { old ->
            recompute(old.copy(sort = old.sort.copy(order = order)))
        }
    }

    /**
     * Resets all filters to their default state.
     */
    fun clearFilters() {
        _uiState.update { old ->
            recompute(old.copy(filters = DealsFilterState()))
        }
    }
}