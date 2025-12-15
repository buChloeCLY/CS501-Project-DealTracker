package com.example.dealtracker.ui.wishlist.viewmodel

import com.google.gson.annotations.SerializedName

/**
 * Data model for a single wishlist item received from the API.
 */
data class WishlistItem(
    val wid: Int,
    val pid: Int,
    val title: String,
    val current_price: Double,
    val target_price: Double?,

    // Use @SerializedName to handle Int (0/1) to Boolean conversion internally
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
    // Boolean properties for UI consumption
    val alert_enabled: Boolean get() = _alert_enabled == 1
    val in_stock: Boolean get() = _in_stock == 1
    val free_shipping: Boolean get() = _free_shipping == 1
    val price_met: Boolean get() = _price_met == 1
}

/**
 * Request body for adding an item to the wishlist.
 */
data class AddWishlistRequest(
    val uid: Int,
    val pid: Int,
    val target_price: Double? = null,
    val alert_enabled: Int = 1,  // Sent as Int to API
    val priority: Int = 2,
    val notes: String? = null
)

/**
 * Request body for updating an existing wishlist item.
 */
data class UpdateWishlistRequest(
    val target_price: Double? = null,
    val alert_enabled: Int? = null,  // Sent as Int to API
    val priority: Int? = null,
    val notes: String? = null
)

/**
 * Generic response for wishlist operations (add, update, delete).
 */
data class WishlistResponse(
    val success: Boolean,
    val message: String? = null,
    val item: WishlistItem? = null
)

/**
 * Response when checking if a product is already in the wishlist.
 */
data class CheckWishlistResponse(
    val in_wishlist: Boolean,
    val item: WishlistItem? = null
)

/**
 * Response containing a list of price alerts.
 */
data class AlertsResponse(
    val total_alerts: Int,
    val alerts: List<WishlistItem>
)

/**
 * Data model for aggregated wishlist statistics.
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