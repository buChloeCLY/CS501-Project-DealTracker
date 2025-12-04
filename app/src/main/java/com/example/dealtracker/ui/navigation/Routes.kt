package com.example.dealtracker.ui.navigation

import android.net.Uri

/**
 * 全局路由定义文件
 * 管理应用中所有可导航的页面路径。
 */
object Routes {

    const val HOME = "home"
    const val DEALS = "deals"
    const val LISTS = "lists"
    const val PROFILE = "profile"
    const val DETAIL_BASE = "detail"

    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val WISHLIST = "wishlist"
    const val EDIT_PROFILE = "edit_profile"

    /**
     * 商品详情页路由
     */
    fun detailRoute(pid: Int, name: String, price: Double, rating: Float): String {
        val encodedName = Uri.encode(name)
        val encodedPrice = Uri.encode(price.toString())
        return "$DETAIL_BASE?pid=$pid&name=$encodedName&price=$encodedPrice&rating=$rating"
    }

    /**
     * Deals 页面带搜索参数
     * 必须 URL 编码，否则空格、特殊字符会破坏导航
     */
    fun dealsWithQuery(query: String): String {
        val encoded = Uri.encode(query)
        return "deals?query=$encoded"
    }
}
