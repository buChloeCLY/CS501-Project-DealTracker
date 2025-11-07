package com.example.dealtracker.ui.wishlist

import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 全局 WishList 管理器，用于添加、删除和清空收藏商品。
 * 使用 StateFlow 以便界面实时更新。
 */
object WishListHolder {
    // 内部可变列表
    private val _wishList = MutableStateFlow<List<Product>>(emptyList())

    // 外部只读的 StateFlow
    val wishList: StateFlow<List<Product>> = _wishList

    /**
     * 添加商品到收藏列表。
     * 若商品已存在（根据 pid 判断），则忽略。
     */
    fun add(product: Product) {
        if (_wishList.value.none { it.pid == product.pid }) {
            _wishList.value = _wishList.value + product
        }
    }

    /**
     * 根据商品 ID 删除指定商品。
     */
    fun remove(pid: Int) {
        _wishList.value = _wishList.value.filterNot { it.pid == pid }
    }

    /**
     * 删除整个收藏列表。
     */
    fun clear() {
        _wishList.value = emptyList()
    }
}
