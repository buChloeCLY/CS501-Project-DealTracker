package com.example.dealtracker.ui.navigation

import android.net.Uri

/**
 * Global object defining all navigable page routes in the application.
 */
object Routes {

    // Root tabs / main destinations
    const val HOME = "home"
    const val DEALS = "deals"
    const val LISTS = "lists"
    const val PROFILE = "profile"

    // Sub routes
    const val DETAIL_BASE = "detail"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val WISHLIST = "wishlist"
    const val EDIT_PROFILE = "edit_profile"

    // AI suggested this
    // Deals sub-routes (separated prefixes to avoid route matching ambiguity)
    const val DEALS_SEARCH = "deals_search"
    const val DEALS_CATEGORY = "deals_category"

    /**
     * Constructs the route string for the product detail page with embedded parameters.
     * @param pid Product ID.
     * @param name Product name.
     * @param price Product price.
     * @param rating Product rating.
     * @return The formatted URL route string.
     */
    fun detailRoute(pid: Int, name: String, price: Double, rating: Float): String {
        val encodedName = Uri.encode(name)
        val encodedPrice = Uri.encode(price.toString())
        return "$DETAIL_BASE?pid=$pid&name=$encodedName&price=$encodedPrice&rating=$rating"
    }

    /**
     * Constructs the route string for the Deals page with a search query parameter.
     * The query must be URL encoded.
     * @param query The search query string.
     * @return The formatted URL route string.
     */
    fun dealsWithQuery(query: String): String {
        val encoded = Uri.encode(query.trim())
        return "$DEALS_SEARCH?query=$encoded"
    }
    /**
     * Constructs the route string for the Deals category entry with a category path parameter.
     * The category is URL encoded.
     */
    fun dealsWithCategory(category: String): String {
        val encoded = Uri.encode(category.trim())
        return "$DEALS_CATEGORY/$encoded"
    }
}