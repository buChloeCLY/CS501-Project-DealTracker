package com.example.dealtracker.ui.wishlist.viewmodel

import com.google.gson.annotations.SerializedName

/**
 * 心愿单项数据模型
 */
data class WishlistItem(
    val wid: Int,
    val pid: Int,
    val title: String,
    val current_price: Double,
    val target_price: Double?,

    // 使用 @SerializedName 让 Gson 自动转换 0/1 为 Boolean
    @SerializedName("alert_enabled")
    private val _alert_enabled: Int = 1,

    val alert_status: Int = 0,
    val priority: Int = 2,
    val notes: String? = null,
    val rating: Float? = null,
    val category: String? = null,
    val image_url: String? = null,

    @SerializedName("in_stock")
    private val _in_stock: Int = 1,

    @SerializedName("free_shipping")
    private val _free_shipping: Int = 0,

    @SerializedName("price_met")
    private val _price_met: Int = 0,

    val savings: Double? = null,
    val created_at: String? = null
) {
    // 提供 Boolean 属性供界面使用
    val alert_enabled: Boolean get() = _alert_enabled == 1
    val in_stock: Boolean get() = _in_stock == 1
    val free_shipping: Boolean get() = _free_shipping == 1
    val price_met: Boolean get() = _price_met == 1
}

/**
 * 添加到心愿单的请求
 */
data class AddWishlistRequest(
    val uid: Int,
    val pid: Int,
    val target_price: Double? = null,
    val alert_enabled: Int = 1,  // 发送时用 Int
    val priority: Int = 2,
    val notes: String? = null
)

/**
 * 更新心愿单的请求
 */
data class UpdateWishlistRequest(
    val target_price: Double? = null,
    val alert_enabled: Int? = null,  // 发送时用 Int
    val priority: Int? = null,
    val notes: String? = null
)

/**
 * 心愿单响应
 */
data class WishlistResponse(
    val success: Boolean,
    val message: String? = null,
    val item: WishlistItem? = null
)

/**
 * 检查心愿单响应
 */
data class CheckWishlistResponse(
    val in_wishlist: Boolean,
    val item: WishlistItem? = null
)

/**
 * 价格提醒响应
 */
data class AlertsResponse(
    val total_alerts: Int,
    val alerts: List<WishlistItem>
)

/**
 * 心愿单统计
 */
data class WishlistStats(
    val total_items: Int,
    val items_on_sale: Int,
    val alerts_enabled: Int? = null,
    val items_in_stock: Int? = null,
    val avg_price: Double?,
    val min_price: Double? = null,
    val max_price: Double? = null,
    val total_savings: Double?
)