package com.example.dealtracker.ui.wishlist.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.api.WishlistItemResponse
import com.example.dealtracker.data.remote.repository.ProductRepositoryImpl
import com.example.dealtracker.data.remote.repository.WishlistRepository
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.wishlist.WishListHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the user's wishlist and price alert operations.
 * @param wishlistRepository Repository for API calls related to wishlist.
 * @param productRepository Repository for fetching full product details.
 */
class WishListViewModel(
    private val wishlistRepository: WishlistRepository = WishlistRepository(),
    private val productRepository: ProductRepositoryImpl = ProductRepositoryImpl()
) : ViewModel() {

    private val TAG = "WishListViewModel"

    val wishList: StateFlow<List<Product>> = WishListHolder.wishList

    // State flow to track target prices set by the user (pid -> targetPrice)
    private val _targetPrices = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val targetPrices: StateFlow<Map<Int, Double>> = _targetPrices.asStateFlow()

    /**
     * Loads the entire wishlist from the backend, fetches product details, and updates local state.
     * @param uid User ID.
     */
    fun loadWishlist(uid: Int) {
        Log.d(TAG, "Loading wishlist for uid=$uid")

        viewModelScope.launch {
            val result = wishlistRepository.getWishlist(uid)

            result.onSuccess { items ->
                Log.d(TAG, "Loaded ${items.size} wishlist items from backend")

                // Clear old data
                WishListHolder.clear()
                val targetPriceMap = mutableMapOf<Int, Double>()

                // Load detailed product information for each wishlist item
                items.forEach { item ->
                    productRepository.getProductById(item.pid)
                        .onSuccess { product ->
                            WishListHolder.add(product)

                            // Save target price
                            item.target_price?.let { targetPrice ->
                                targetPriceMap[item.pid] = targetPrice
                            }

                            Log.d(TAG, "Loaded product: ${product.title}, target: ${item.target_price}")
                        }
                        .onFailure { e ->
                            Log.e(TAG, "Failed to load product ${item.pid}: ${e.message}")
                        }
                }

                // Update target prices state
                _targetPrices.value = targetPriceMap

            }.onFailure { e ->
                Log.e(TAG, "Failed to load wishlist: ${e.message}")
            }
        }
    }

    /**
     * Adds a product to the wishlist (if not already present).
     * @param uid User ID.
     * @param product The Product domain model.
     * @param alertEnabled Whether price alerts should be enabled (default true).
     */
    fun addProduct(
        uid: Int,
        product: Product,
        alertEnabled: Boolean = true
    ) {
        if (!WishListHolder.contains(product.pid)) {
            WishListHolder.add(product)
        }

        viewModelScope.launch {
            wishlistRepository.upsertWishlist(
                uid = uid,
                pid = product.pid,
                targetPrice = null,
                alertEnabled = alertEnabled
            ).onSuccess {
                Log.d(TAG, "Added to wishlist without target_price")
            }.onFailure { e ->
                Log.e(TAG, "Failed to add: ${e.message}")
            }
        }
    }

    /**
     * Updates the target price for a product in the wishlist.
     * @param uid User ID.
     * @param pid Product ID.
     * @param targetPrice The new target price.
     * @param alertEnabled Whether price alerts should be enabled (default true).
     * @param onSuccess Callback invoked with a boolean indicating if the price was already met.
     */
    fun updateTargetPrice(
        uid: Int,
        pid: Int,
        targetPrice: Double,
        alertEnabled: Boolean = true,
        onSuccess: ((priceReached: Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = wishlistRepository.upsertWishlist(
                uid = uid,
                pid = pid,
                targetPrice = targetPrice,
                alertEnabled = alertEnabled
            )

            result.onSuccess { response ->
                // Update local target price
                _targetPrices.value = _targetPrices.value + (pid to targetPrice)

                if (response.priceReached == true) {
                    Log.d(TAG, "Price already reached for pid=$pid, will trigger notification")
                    onSuccess?.invoke(true)
                } else {
                    Log.d(TAG, "Price not reached yet for pid=$pid")
                    onSuccess?.invoke(false)
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to update target price: ${e.message}")
                onSuccess?.invoke(false)
            }
        }
    }

    /**
     * Removes a product from the local memory and the backend wishlist.
     * @param uid User ID.
     * @param pid Product ID.
     */
    fun removeProduct(uid: Int, pid: Int) {
        Log.d(TAG, "Removing product: pid=$pid")

        // 1. Remove from local memory
        WishListHolder.remove(pid)

        // 2. Clear target price
        _targetPrices.value = _targetPrices.value - pid
        Log.d(TAG, "Cleared target price for pid=$pid")

        // 3. Delete from backend
        viewModelScope.launch {
            wishlistRepository.deleteWishlist(uid, pid)
                .onSuccess {
                    Log.d(TAG, "Deleted from backend: pid=$pid")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to delete from backend: ${e.message}")
                }
        }
    }

    /**
     * Checks for price drop alerts from the backend.
     * Alerts are automatically marked as notified after the callback.
     * @param uid User ID.
     * @param onAlerts Callback invoked with the list of active alerts.
     */
    fun checkAlerts(
        uid: Int,
        onAlerts: (List<WishlistItemResponse>) -> Unit
    ) {
        viewModelScope.launch {
            val result = wishlistRepository.getAlerts(uid)
            result.onSuccess { alerts ->
                if (alerts.isNotEmpty()) {
                    Log.d(TAG, "Found ${alerts.size} alerts to notify")

                    // 1. Trigger notification callback
                    onAlerts(alerts)

                    // 2. Mark as notified after processing
                    alerts.forEach { alert ->
                        markAsNotified(uid, alert.pid)
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to check alerts: ${e.message}")
            }
        }
    }

    /**
     * Marks a price alert as 'notified' on the backend.
     * @param uid User ID.
     * @param pid Product ID.
     */
    private fun markAsNotified(uid: Int, pid: Int) {
        viewModelScope.launch {
            wishlistRepository.markNotified(uid, pid)
                .onSuccess {
                    Log.d(TAG, "Marked as notified: pid=$pid")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to mark notified: ${e.message}")
                }
        }
    }

    /**
     * Marks a price alert as 'read' after the user clicks the notification.
     * @param uid User ID.
     * @param pid Product ID.
     */
    fun markAsRead(uid: Int, pid: Int) {
        viewModelScope.launch {
            wishlistRepository.markRead(uid, pid)
                .onSuccess {
                    Log.d(TAG, "Marked as read: pid=$pid, will not notify again")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to mark read: ${e.message}")
                }
        }
    }

    /**
     * Retrieves the locally stored target price for a given product ID.
     * @param pid Product ID.
     * @return The target price, or null if not set.
     */
    fun getTargetPrice(pid: Int): Double? {
        return _targetPrices.value[pid]
    }
}