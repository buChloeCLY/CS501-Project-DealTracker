package com.example.dealtracker.domain.repository

import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint

/**
 * Interface defining the contract for accessing price data.
 */
interface PriceRepository {
    /**
     * Retrieves prices for a specified product across all platforms.
     * @param pid Product ID.
     * @return List of PlatformPrice objects.
     */
    suspend fun getPlatformPrices(pid: Int): List<PlatformPrice>

    /**
     * Retrieves the historical price data for a specified product.
     * @param pid Product ID.
     * @param days Number of days for the history query.
     * @return List of PricePoint objects.
     */
    suspend fun getPriceHistory(pid: Int, days: Int): List<PricePoint>
}