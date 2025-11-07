package com.example.dealtracker.ui.wishlist

import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WishListHolder {
    private val _wishList = MutableStateFlow<List<Product>>(emptyList())
    val wishList: StateFlow<List<Product>> get() = _wishList

    fun add(product: Product) {
        if (_wishList.value.none { it.pid == product.pid }) {
            _wishList.value = _wishList.value + product
        }
    }
}
