package com.example.dealtrackerv1.viewmodel

import androidx.lifecycle.ViewModel
import com.example.dealtrackerv1.model.PlatformPrice
import com.example.dealtrackerv1.model.PricePoint
import com.example.dealtrackerv1.model.Product

class ProductViewModel : ViewModel() {

    fun getProduct(): Product {
        return Product(
            name = "iPhone 16",
            color = "White",
            storage = "256G",
            currentPrice = 999,
            originalPrice = 1999,
            imageUrl = "" // 后面如果要加 Coil/Glide 再替换
        )
    }

    fun getPriceHistory(): List<PricePoint> {
        return listOf(
            PricePoint("04-02", 1299),
            PricePoint("04-10", 1200),
            PricePoint("04-14", 1179),
            PricePoint("04-20", 1150),
            PricePoint("04-25", 1100),
            PricePoint("05-28", 999)
        )
    }

    fun getPlatformPrices(): List<PlatformPrice> {
        return listOf(
            PlatformPrice("Amazon", "amazon", 999),
            PlatformPrice("Apple", "apple", 999),
        )
    }
}
