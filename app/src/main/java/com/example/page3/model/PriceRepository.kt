package com.example.page3.repository

import com.example.page3.model.HistoryPointDto
import com.example.page3.model.PlatformPrice
import com.example.page3.model.PriceDto
import com.example.page3.model.PricePoint
import com.example.page3.network.RetrofitClient
class PriceRepository {

    suspend fun getPlatformPrices(pid: Int): List<PlatformPrice> {
        return try {
            val dtoList: List<PriceDto> = RetrofitClient.api.getPrices(pid)
            dtoList.map {
                PlatformPrice(
                    platformName = it.platform,
                    platformIcon = "",   // 后面可以做图标映射
                    price = it.price
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPriceHistory(pid: Int, days: Int): List<HistoryPointDto> {
        return try {
            val result = RetrofitClient.api.getHistory(pid, days)
            println("🔍 Repository getPriceHistory(pid=$pid) -> ${result.size} 条")
            result
        } catch (e: Exception) {
            println("❌ Repository error: ${e}")
            emptyList()
        }
    }

}
