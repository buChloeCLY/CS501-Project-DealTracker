package com.example.dealtracker.ui.wishlist

import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.model.WishlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 全局 WishList 管理器
 * 支持本地缓存 + 后端同步
 */
object WishListHolder {
    // 本地缓存 - 用于快速访问和离线使用
    private val _localWishList = MutableStateFlow<List<Product>>(emptyList())
    val localWishList: StateFlow<List<Product>> = _localWishList

    // 后端数据 - 完整的心愿单数据（包含目标价格等）
    private val _remoteWishList = MutableStateFlow<List<WishlistItem>>(emptyList())
    val remoteWishList: StateFlow<List<WishlistItem>> = _remoteWishList

    /**
     * 添加商品到本地缓存（即时生效）
     */
    fun addLocal(product: Product) {
        if (_localWishList.value.none { it.pid == product.pid }) {
            _localWishList.value = _localWishList.value + product
        }
    }

    /**
     * 从本地缓存删除
     */
    fun removeLocal(pid: Int) {
        _localWishList.value = _localWishList.value.filterNot { it.pid == pid }
    }

    /**
     * 清空本地缓存
     */
    fun clearLocal() {
        _localWishList.value = emptyList()
    }

    /**
     * 清空所有数据（退出登录时使用）
     */
    fun clearAll() {
        _localWishList.value = emptyList()
        _remoteWishList.value = emptyList()
    }

    /**
     * 检查商品是否在本地缓存中
     */
    fun isInLocal(pid: Int): Boolean {
        return _localWishList.value.any { it.pid == pid }
    }

    /**
     * 更新后端数据（从 API 获取后更新）
     */
    fun updateRemote(items: List<WishlistItem>) {
        _remoteWishList.value = items
        syncLocalFromRemote(items)
    }

    /**
     * 从后端数据同步到本地缓存
     */
    private fun syncLocalFromRemote(items: List<WishlistItem>) {
        val localProducts = items.map { item ->
            Product(
                pid = item.pid,
                title = item.title,
                price = item.current_price,
                rating = item.rating ?: 0f,
                platform = com.example.dealtracker.domain.model.Platform.Amazon,
                freeShipping = item.free_shipping,
                inStock = item.in_stock,
                imageUrl = item.image_url ?: "",
                category = parseCategoryFromString(item.category),
                information = item.notes
            )
        }
        _localWishList.value = localProducts
    }

    /**
     * 获取完整的心愿单项（包含目标价格等信息）
     */
    fun getRemoteItem(pid: Int): WishlistItem? {
        return _remoteWishList.value.firstOrNull { it.pid == pid }
    }

    /**
     * 辅助函数：从字符串解析分类
     */
    private fun parseCategoryFromString(category: String?): com.example.dealtracker.domain.model.Category {
        return when (category?.lowercase()) {
            "electronics" -> com.example.dealtracker.domain.model.Category.Electronics
            "beauty" -> com.example.dealtracker.domain.model.Category.Beauty
            "home" -> com.example.dealtracker.domain.model.Category.Home
            "food" -> com.example.dealtracker.domain.model.Category.Food
            "fashion" -> com.example.dealtracker.domain.model.Category.Fashion
            "sports" -> com.example.dealtracker.domain.model.Category.Sports
            "books" -> com.example.dealtracker.domain.model.Category.Books
            "toys" -> com.example.dealtracker.domain.model.Category.Toys
            "health" -> com.example.dealtracker.domain.model.Category.Health
            "outdoors" -> com.example.dealtracker.domain.model.Category.Outdoors
            "office" -> com.example.dealtracker.domain.model.Category.Office
            "pets" -> com.example.dealtracker.domain.model.Category.Pets
            else -> com.example.dealtracker.domain.model.Category.Electronics
        }
    }
}