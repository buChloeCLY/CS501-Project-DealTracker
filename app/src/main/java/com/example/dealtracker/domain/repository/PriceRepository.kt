package com.example.dealtracker.domain.repository

import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint

// 价格数据仓库接口（领域层定义）
interface PriceRepository {
    /**
     * 获取指定产品在所有平台的价格
     * @param pid 产品ID
     * @return 平台价格列表
     */
    suspend fun getPlatformPrices(pid: Int): List<PlatformPrice>

    /**
     * 获取指定产品的价格历史
     * @param pid 产品ID
     * @param days 查询天数
     * @return 价格历史点列表
     */
    suspend fun getPriceHistory(pid: Int, days: Int): List<PricePoint>
}