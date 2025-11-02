package com.example.dealtracker.data.remote.api

import com.example.dealtracker.data.remote.dto.HistoryPriceDto
import com.example.dealtracker.data.remote.dto.PriceDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ✅ 价格相关API接口
interface PriceApi {
    /**
     * 获取指定产品在所有平台的价格记录
     * @param pid 产品ID
     * @return 平台价格记录列表
     */
    @GET("price/{pid}")
    suspend fun getPrices(@Path("pid") pid: Int): List<PriceDto>

    /**
     * 获取指定产品的历史价格数据
     * @param pid 产品ID
     * @param days 查询天数，默认7天
     * @return 历史价格点列表
     */
    @GET("history/{pid}")
    suspend fun getHistory(
        @Path("pid") pid: Int,
        @Query("days") days: Int = 7
    ): List<HistoryPriceDto>
}