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

class WishListViewModel(
    private val wishlistRepository: WishlistRepository = WishlistRepository(),
    private val productRepository: ProductRepositoryImpl = ProductRepositoryImpl()
) : ViewModel() {

    private val TAG = "WishListViewModel"

    val wishList: StateFlow<List<Product>> = WishListHolder.wishList

    // ⭐ 添加 target price 状态管理
    private val _targetPrices = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val targetPrices: StateFlow<Map<Int, Double>> = _targetPrices.asStateFlow()

    /**
     * ⭐ 从后端加载 Wishlist（登录时或启动时调用）
     */
    fun loadWishlist(uid: Int) {
        Log.d(TAG, "🔄 Loading wishlist for uid=$uid")

        viewModelScope.launch {
            val result = wishlistRepository.getWishlist(uid)

            result.onSuccess { items ->
                Log.d(TAG, "✅ Loaded ${items.size} wishlist items from backend")

                // ⭐ 清空旧数据
                WishListHolder.clear()
                val targetPriceMap = mutableMapOf<Int, Double>()

                // ⭐ 加载每个产品的详细信息
                items.forEach { item ->
                    // 从后端获取完整的产品信息
                    productRepository.getProductById(item.pid)
                        .onSuccess { product ->
                            WishListHolder.add(product)

                            // ⭐ 保存 target price
                            item.target_price?.let { targetPrice ->
                                targetPriceMap[item.pid] = targetPrice
                            }

                            Log.d(TAG, "✅ Loaded product: ${product.title}, target: ${item.target_price}")
                        }
                        .onFailure { e ->
                            Log.e(TAG, "Failed to load product ${item.pid}: ${e.message}")
                        }
                }

                // ⭐ 更新 target prices
                _targetPrices.value = targetPriceMap

            }.onFailure { e ->
                Log.e(TAG, "Failed to load wishlist: ${e.message}")
            }
        }
    }

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
                Log.d(TAG, "✅ Added to wishlist without target_price")
            }.onFailure { e ->
                Log.e(TAG, "Failed to add: ${e.message}")
            }
        }
    }

    /**
     * 在 wishlist 页面修改目标价格
     * ⭐ 添加回调，通知 Screen 是否达标
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
                // ⭐ 更新本地 target price
                _targetPrices.value = _targetPrices.value + (pid to targetPrice)

                if (response.priceReached == true) {
                    Log.d(TAG, "✅ Price already reached for pid=$pid, will trigger notification")
                    onSuccess?.invoke(true)
                } else {
                    Log.d(TAG, "⏳ Price not reached yet for pid=$pid")
                    onSuccess?.invoke(false)
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to update target price: ${e.message}")
                onSuccess?.invoke(false)
            }
        }
    }

    /**
     * ⭐ 删除商品（同时清除 target price）
     */
    fun removeProduct(uid: Int, pid: Int) {
        Log.d(TAG, "🗑️ Removing product: pid=$pid")

        // ⭐ 1. 从本地内存删除
        WishListHolder.remove(pid)

        // ⭐ 2. 清除 target price（重要！）
        _targetPrices.value = _targetPrices.value - pid
        Log.d(TAG, "✅ Cleared target price for pid=$pid")

        // ⭐ 3. 从后端删除
        viewModelScope.launch {
            wishlistRepository.deleteWishlist(uid, pid)
                .onSuccess {
                    Log.d(TAG, "✅ Deleted from backend: pid=$pid")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to delete from backend: ${e.message}")
                }
        }
    }

    /**
     * 检查降价提醒
     * ⭐ 推送完成后自动标记为已推送
     */
    fun checkAlerts(
        uid: Int,
        onAlerts: (List<WishlistItemResponse>) -> Unit
    ) {
        viewModelScope.launch {
            val result = wishlistRepository.getAlerts(uid)
            result.onSuccess { alerts ->
                if (alerts.isNotEmpty()) {
                    Log.d(TAG, "🔔 Found ${alerts.size} alerts to notify")

                    // 1. 触发通知回调
                    onAlerts(alerts)

                    // 2. ⭐ 推送完成后标记为已推送
                    alerts.forEach { alert ->
                        markAsNotified(uid, alert.pid)
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to check alerts: ${e.message}")
            }
        }
    }

    /** ⭐ 标记为已推送 */
    private fun markAsNotified(uid: Int, pid: Int) {
        viewModelScope.launch {
            wishlistRepository.markNotified(uid, pid)
                .onSuccess {
                    Log.d(TAG, "✅ Marked as notified: pid=$pid")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to mark notified: ${e.message}")
                }
        }
    }

    /** ⭐ 标记为已读（点击通知后调用） */
    fun markAsRead(uid: Int, pid: Int) {
        viewModelScope.launch {
            wishlistRepository.markRead(uid, pid)
                .onSuccess {
                    Log.d(TAG, "✅ Marked as read: pid=$pid, will not notify again")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to mark read: ${e.message}")
                }
        }
    }

    /**
     * ⭐ 获取指定产品的 target price
     */
    fun getTargetPrice(pid: Int): Double? {
        return _targetPrices.value[pid]
    }
}