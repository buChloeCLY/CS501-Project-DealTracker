package com.example.dealtracker.ui.navigation

import android.net.Uri

/**
 * 全局路由定义文件
 * 管理应用中所有可导航的页面路径。
 * 通过集中定义路由常量，保证不同模块间的跳转一致性。
 */
object Routes {
    // === 主页面路由 ===
    const val HOME = "home"       // 首页：搜索、分类、今日特价
    const val DEALS = "deals"     // Deals页：按类别浏览所有商品
    const val LISTS = "lists"     // WishList页：收藏、提醒、目标价设置
    const val PROFILE = "profile" // 个人页：账户、设置等
    const val DETAIL_BASE = "detail" // 商品详情页（带参数）

    /**
     * 生成商品详情页的完整路由字符串。
     *
     * 示例：
     * detail?pid=123&name=Phone%20X&price=899.99&rating=4.5
     *
     * @param pid 商品ID
     * @param name 商品名称（需URL编码）
     * @param price 当前价格
     * @param rating 商品评分
     */
    fun detailRoute(pid: Int, name: String, price: Double, rating: Float): String {
        val encodedName = Uri.encode(name)
        val encodedPrice = Uri.encode(price.toString())
        return "$DETAIL_BASE?pid=$pid&name=$encodedName&price=$encodedPrice&rating=$rating"
    }
}
