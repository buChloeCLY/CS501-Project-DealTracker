package com.example.dealtracker.data.remote.api

import com.example.dealtracker.data.remote.dto.HistoryPriceDto
import com.example.dealtracker.data.remote.dto.PriceDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PriceApi {
    /**
     * Gets the price records for a specified product across all platforms.
     * @param pid Product ID.
     */
    @GET("price/{pid}")
    suspend fun getPrices(@Path("pid") pid: Int): List<PriceDto>

    /**
     * Gets historical price data for a specified product.
     * @param pid Product ID.
     * @param days Number of days to query, defaults to 7 days.
     */
    @GET("history/{pid}")
    suspend fun getHistory(
        @Path("pid") pid: Int,
        @Query("days") days: Int = 7
    ): List<HistoryPriceDto>
}