package com.example.dealtracker.ui.wishlist

import androidx.lifecycle.ViewModel
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WishListViewModel : ViewModel() {
    private val _wishList = MutableStateFlow<List<Product>>(emptyList())
    val wishList: StateFlow<List<Product>> = _wishList

    fun addProduct(product: Product) {
        if (_wishList.value.none { it.pid == product.pid }) {
            _wishList.value = _wishList.value + product
        }
    }

    fun updateTargetPrice(pid: Int, newPrice: Double) {
        _wishList.value = _wishList.value.map {
            if (it.pid == pid) it.copy(price = newPrice) else it
        }
    }
}
