package com.example.dealtracker.ui.wishlist

import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global WishList manager:
 * Handles the in-memory list of Product objects.
 * Synchronization with the backend/database is managed by the ViewModel and Repository.
 */
object WishListHolder {

    private val _wishList = MutableStateFlow<List<Product>>(emptyList())
    val wishList: StateFlow<List<Product>> = _wishList

    /**
     * Adds a product to the local wishlist if it does not already exist.
     * @param product The Product object to add.
     */
    fun add(product: Product) {
        if (_wishList.value.any { it.pid == product.pid }) return
        _wishList.value = _wishList.value + product
    }

    /**
     * Checks if a product with the given ID exists in the local wishlist.
     * @param pid Product ID.
     * @return True if the product is present, false otherwise.
     */
    fun contains(pid: Int): Boolean =
        _wishList.value.any { it.pid == pid }

    /**
     * Removes a product from the local wishlist based on its ID.
     * @param pid Product ID.
     */
    fun remove(pid: Int) {
        _wishList.value = _wishList.value.filterNot { it.pid == pid }
    }

    /**
     * Clears all products from the local wishlist.
     */
    fun clear() {
        _wishList.value = emptyList()
    }
}