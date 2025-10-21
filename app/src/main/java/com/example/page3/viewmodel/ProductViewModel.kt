package com.example.page3.viewmodel

import androidx.lifecycle.ViewModel
import com.example.page3.model.PlatformPrice
import com.example.page3.model.PricePoint
import com.example.page3.model.Product

class ProductViewModel : ViewModel() {

    fun getProduct(): Product {
        return Product(
            name = "iPhone 16",
            color = "White",
            storage = "256G",
            currentPrice = 999.0,
            originalPrice = 1999.0,
            imageUrl = "" // 后面如果要加 Coil/Glide 再替换
        )
    }

    fun getPriceHistory(): List<PricePoint> {
        return listOf(
            PricePoint("04-02", 1299.0),
            PricePoint("04-10", 1200.0),
            PricePoint("04-14", 1179.0),
            PricePoint("04-20", 1150.0),
            PricePoint("04-25", 1100.0),
            PricePoint("05-28", 999.0)
        )
    }

    fun getPlatformPrices(): List<PlatformPrice> {
        return listOf(
            PlatformPrice("Amazon", "amazon", 999.0),
            PlatformPrice("Apple", "apple", 999.0),
        )
    }
}
