package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.dto.HistoryPriceDto
import com.example.dealtracker.data.remote.dto.PriceDto
import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint
import com.example.dealtracker.domain.repository.PriceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PriceRepositoryImpl: PriceRepository {

    private val api = RetrofitClient.priceApi

    override suspend fun getPlatformPrices(pid: Int): List<PlatformPrice> {
        return withContext(Dispatchers.IO) {
            try {
                val dtoList: List<PriceDto> = api.getPrices(pid)
                dtoList.map { dto ->
                    PlatformPrice(
                        platformName = dto.platform,
                        platformIcon = mapPlatformToIcon(dto.platform),
                        price = dto.price,
                        link = dto.link
                    )
                }
            } catch (e: Exception) {
                println("PriceRepository.getPlatformPrices error: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun getPriceHistory(pid: Int, days: Int): List<PricePoint> {
        return withContext(Dispatchers.IO) {
            try {
                val result: List<HistoryPriceDto> = api.getHistory(pid, days)
                println("Repository getPriceHistory(pid=$pid) -> ${result.size} records")

                result.map { dto ->
                    PricePoint(
                        date = dto.date,
                        price = dto.price
                    )
                }
            } catch (e: Exception) {
                println("PriceRepository.getPriceHistory error: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * icons of platforms
     */
    private fun mapPlatformToIcon(platformName: String): String {
        return when (platformName.lowercase()) {
            "amazon" -> "ic_amazon"
            "bestbuy" -> "ic_bestbuy"
            "walmart" -> "ic_walmart"
            "target" -> "ic_target"
            else -> "ic_store_default"
        }
    }
}