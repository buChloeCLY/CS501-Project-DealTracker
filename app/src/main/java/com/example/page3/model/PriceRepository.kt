package com.example.page3.repository

import com.example.page3.model.PlatformPrice
import com.example.page3.network.RetrofitClient
import com.example.page3.model.PriceDto

class PriceRepository {
    suspend fun getPlatformPrices(pid: Int): List<PlatformPrice> {
        val dtoList: List<PriceDto> = RetrofitClient.api.getPrices(pid)
        return dtoList.map {
            PlatformPrice(
                platformName = it.platform,
                platformIcon = "",  // 之后我们再映射 icon
                price = it.price
            )
        }
    }
}
