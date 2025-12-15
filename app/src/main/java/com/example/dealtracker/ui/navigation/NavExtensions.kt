package com.example.dealtracker.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Simplified navigation function to navigate to a root-level route.
 * It clears the back stack up to the start destination (non-inclusive) and avoids duplicate destinations.
 * @param route The destination route, potentially including query parameters.
 */
fun NavHostController.navigateToRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
        }
        launchSingleTop = true     // Avoids duplicating the destination on the stack
    }
}