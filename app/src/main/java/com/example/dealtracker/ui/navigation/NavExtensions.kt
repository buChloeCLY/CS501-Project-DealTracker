package com.example.dealtracker.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Navigates to a root-level destination and clears the back stack up to the start destination.
 *
 * @param route Destination route, which may include parameters.
 */
fun NavHostController.navigateToRoot(route: String) {

    // Parameterized Deals routes should create a new destination to correctly handle parameters.
    val isParameterizedDeals =
        route.startsWith("${Routes.DEALS_SEARCH}?") || route.startsWith("${Routes.DEALS_CATEGORY}/")

    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
        }
        // Use singleTop for static root destinations; disable for parameterized Deals routes.
        launchSingleTop = !isParameterizedDeals
    }
}
