package com.example.dealtracker.ui.navigation

import android.net.Uri

// ✅ 应用路由定义
object Routes {
    // 主页面路由
    const val HOME = "home"
    const val DEALS = "deals"
    const val LISTS = "lists"
    const val PROFILE = "profile"
    const val DETAIL_BASE = "detail"

    /**
     * 生成商品详情页路由
     * @param pid 商品ID
     * @param name 商品名称
     * @param price 商品价格
     * @param rating 商品评分
     */
    fun detailRoute(pid: Int, name: String, price: Double, rating: Float): String {
        val encodedName = Uri.encode(name)
        val encodedPrice = Uri.encode(price.toString())
        return "$DETAIL_BASE?pid=$pid&name=$encodedName&price=$encodedPrice&rating=$rating"
    }
}