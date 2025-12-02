package com.example.dealtracker.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.repository.WishlistRepository
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.model.WishlistItem
import com.example.dealtracker.domain.model.WishlistStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 心愿单 ViewModel
 */
class WishlistViewModel : ViewModel() {

    private val repository = WishlistRepository()

    // 使用 WishListHolder 的数据
    val wishlist: StateFlow<List<WishlistItem>> = WishListHolder.remoteWishList
    val localWishlist: StateFlow<List<Product>> = WishListHolder.localWishList

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 统计信息
    private val _stats = MutableStateFlow<WishlistStats?>(null)
    val stats: StateFlow<WishlistStats?> = _stats

    /**
     * 加载用户心愿单（从后端同步）
     */
    fun loadWishlist(uid: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getWishlist(uid).fold(
                onSuccess = { items ->
                    WishListHolder.updateRemote(items)
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to load wishlist"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * 添加到心愿单
     * 1. 先更新本地缓存（即时生效）
     * 2. 再同步到后端
     */
    fun addToWishlist(
        uid: Int,
        product: Product,
        targetPrice: Double? = null,
        alertEnabled: Boolean = true,
        priority: Int = 2,
        notes: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 1. 立即更新本地（即时响应）
        WishListHolder.addLocal(product)
        onSuccess() // 立即反馈成功

        // 2. 后台同步到服务器
        viewModelScope.launch {
            repository.addToWishlist(
                uid,
                product.pid,
                targetPrice,
                alertEnabled,
                priority,
                notes
            ).fold(
                onSuccess = { response ->
                    if (response.success) {
                        loadWishlist(uid)
                    } else {
                        _error.value = "Added locally, sync failed: ${response.message}"
                    }
                },
                onFailure = { e ->
                    _error.value = "Added locally, will sync later"
                }
            )
        }
    }

    /**
     * 更新心愿单项
     */
    fun updateWishlistItem(
        uid: Int,
        wid: Int,
        targetPrice: Double? = null,
        alertEnabled: Boolean? = null,
        priority: Int? = null,
        notes: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateWishlist(wid, targetPrice, alertEnabled, priority, notes).fold(
                onSuccess = { response ->
                    if (response.success) {
                        loadWishlist(uid)
                        onSuccess()
                    } else {
                        onError(response.message ?: "Failed to update")
                    }
                },
                onFailure = { e ->
                    onError(e.message ?: "Network error")
                }
            )
        }
    }

    /**
     * 从心愿单删除
     * 1. 先更新本地缓存
     * 2. 再同步到后端
     */
    fun removeFromWishlist(
        uid: Int,
        pid: Int,
        wid: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 1. 立即更新本地
        WishListHolder.removeLocal(pid)
        onSuccess() // 立即反馈成功

        // 2. 后台同步到服务器
        viewModelScope.launch {
            repository.removeFromWishlist(wid).fold(
                onSuccess = { response ->
                    if (response.success) {
                        loadWishlist(uid)
                    }
                },
                onFailure = { e ->
                    _error.value = "Removed locally, will sync later"
                }
            )
        }
    }

    /**
     * 检查商品是否在心愿单中
     */
    fun checkInWishlist(
        uid: Int,
        pid: Int,
        onResult: (Boolean, WishlistItem?) -> Unit
    ) {
        // 先检查本地
        val inLocal = WishListHolder.isInLocal(pid)
        val remoteItem = WishListHolder.getRemoteItem(pid)

        if (inLocal) {
            onResult(true, remoteItem)
        } else {
            viewModelScope.launch {
                repository.checkWishlist(uid, pid).fold(
                    onSuccess = { response ->
                        onResult(response.in_wishlist, response.item)
                    },
                    onFailure = {
                        onResult(false, null)
                    }
                )
            }
        }
    }

    /**
     * 加载统计信息
     */
    fun loadStats(uid: Int) {
        viewModelScope.launch {
            repository.getStats(uid).fold(
                onSuccess = { stats ->
                    _stats.value = stats
                },
                onFailure = { e ->
                    _stats.value = null
                }
            )
        }
    }

    /**
     * 清空错误信息
     */
    fun clearError() {
        _error.value = null
    }
}