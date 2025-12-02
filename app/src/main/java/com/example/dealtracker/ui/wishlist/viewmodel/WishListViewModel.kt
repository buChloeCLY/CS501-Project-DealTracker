package com.example.dealtracker.ui.wishlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.api.WishlistItemResponse
import com.example.dealtracker.data.remote.repository.WishlistRepository
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.wishlist.WishListHolder
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WishListViewModel(
    private val repository: WishlistRepository = WishlistRepository()
) : ViewModel() {

    val wishList: StateFlow<List<Product>> = WishListHolder.wishList

    /** 从其他页面加入商品（可选直接带 targetPrice） */
    fun addProduct(
        uid: Int,
        product: Product,
        targetPrice: Double? = null,
        alertEnabled: Boolean = true
    ) {
        if (!WishListHolder.contains(product.pid)) {
            WishListHolder.add(product)
        }
        viewModelScope.launch {
            repository.upsertWishlist(
                uid = uid,
                pid = product.pid,
                targetPrice = targetPrice,
                alertEnabled = alertEnabled
            )
        }
    }

    /** 在 wishlist 页面修改目标价格 */
    fun updateTargetPrice(
        uid: Int,
        pid: Int,
        targetPrice: Double,
        alertEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            repository.upsertWishlist(
                uid = uid,
                pid = pid,
                targetPrice = targetPrice,
                alertEnabled = alertEnabled
            )
        }
    }

    /** 删除某个商品 */
    fun removeProduct(uid: Int, pid: Int) {
        WishListHolder.remove(pid)
        viewModelScope.launch {
            repository.deleteWishlist(uid, pid)
        }
    }

    /** 检查降价提醒：调用后端 /api/wishlist/alerts */
    fun checkAlerts(
        uid: Int,
        onAlerts: (List<WishlistItemResponse>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.getAlerts(uid)
            result.onSuccess { alerts ->
                if (alerts.isNotEmpty()) {
                    onAlerts(alerts)
                }
            }
        }
    }
}
