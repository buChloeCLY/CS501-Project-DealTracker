package com.example.dealtracker.ui.wishlist

import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 全局 WishList 管理器：
 *  - 只负责本地内存中的 Product 列表
 *  - 和后端 / 数据库同步由 ViewModel + Repository 负责
 */
object WishListHolder {

    private val _wishList = MutableStateFlow<List<Product>>(emptyList())
    val wishList: StateFlow<List<Product>> = _wishList

    /** 添加到本地 wishlist（如果不存在） */
    fun add(product: Product) {
        if (_wishList.value.any { it.pid == product.pid }) return
        _wishList.value = _wishList.value + product
    }

    /** 判断是否已存在 */
    fun contains(pid: Int): Boolean =
        _wishList.value.any { it.pid == pid }

    /** 根据 pid 删除 */
    fun remove(pid: Int) {
        _wishList.value = _wishList.value.filterNot { it.pid == pid }
    }

    /** 清空 */
    fun clear() {
        _wishList.value = emptyList()
    }
}
