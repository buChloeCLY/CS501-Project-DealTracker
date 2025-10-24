package com.example.page3.network

import com.example.page3.model.PriceDto
import com.example.page3.model.HistoryPointDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PriceApi {
    // 已有：平台价格原始记录列表
    @GET("price/{pid}")
    suspend fun getPrices(@Path("pid") pid: Int): List<PriceDto>

    // ✅ 新增：历史最低价（短日期）
    @GET("history/{pid}")
    suspend fun getHistory(
        @Path("pid") pid: Int,
        @Query("days") days: Int = 7
    ): List<HistoryPointDto>
}
