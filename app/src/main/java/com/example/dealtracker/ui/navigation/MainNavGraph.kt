package com.example.dealtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.dealtracker.ui.deals.DealsScreen
import com.example.dealtracker.ui.detail.ProductDetailScreen
import com.example.dealtracker.ui.detail.ProductDetailScreenWithPid
import com.example.dealtracker.ui.home.HomeScreen
import com.example.dealtracker.ui.wishlist.WishListScreen
import com.example.dealtracker.ui.profile.SettingsScreen
import com.example.dealtracker.ui.profile.ProfileScreen
import com.example.dealtracker.ui.profile.HistoryScreen
import com.example.dealtracker.ui.profile.EditProfileScreen
import com.example.dealtracker.ui.profile.LoginScreen
import com.example.dealtracker.ui.profile.RegisterScreen
import com.example.dealtracker.domain.UserManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue



/**
 * Main navigation graph configuration using Navigation Compose.
 * Defines all screen routes and their arguments.
 * @param navController The host controller for navigation operations.
 * @param modifier Modifier for styling the NavHost.
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // NavHost: Sets the start destination (Home)
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {

        // ============= Home Screen =============
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }

// ============= Deals Screen (Default entry) =============
        composable(Routes.DEALS) {
            DealsScreen(
                showBack = navController.previousBackStackEntry != null,
                onBack = { navController.popBackStack() },
                searchQuery = null,
                category = null,
                onCompareClick = { product ->
                    navController.navigate(
                        Routes.detailRoute(
                            pid = product.pid,
                            name = product.title,
                            price = product.price,
                            rating = product.rating
                        )
                    )
                }
            )
        }

// ============= Deals Screen (Category entry) =============
        composable(
            route = "deals/{category}",
            arguments = listOf(
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val category = backStackEntry.arguments?.getString("category")

            DealsScreen(
                showBack = navController.previousBackStackEntry != null,
                onBack = { navController.popBackStack() },
                searchQuery = null,
                category = category, // Use category parameter
                onCompareClick = { product ->
                    navController.navigate(
                        Routes.detailRoute(
                            pid = product.pid,
                            name = product.title,
                            price = product.price,
                            rating = product.rating
                        )
                    )
                }
            )
        }


// ============= Deals Screen (Search entry) =============
        composable(
            route = "deals?query={query}",
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->

            val query = backStackEntry.arguments?.getString("query")

            DealsScreen(
                showBack = navController.previousBackStackEntry != null,
                onBack = { navController.popBackStack() },
                searchQuery = query,
                category = null,  // Category is null during search
                onCompareClick = { product ->
                    navController.navigate(
                        Routes.detailRoute(
                            pid = product.pid,
                            name = product.title,
                            price = product.price,
                            rating = product.rating
                        )
                    )
                }
            )
        }

        // ============= Product Detail Screen (pid only - for Deep Link/Notification) =============
        composable(
            route = "detail/{pid}",
            arguments = listOf(
                navArgument("pid") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getInt("pid") ?: 1

            // Use component that loads product details from API
            ProductDetailScreenWithPid(
                pid = pid,
                navController = navController
            )
        }

        // ============= Product Detail Screen (Full parameters - backward compatible) =============
        composable(
            route = Routes.DETAIL_BASE +
                    "?pid={pid}&name={name}&price={price}&rating={rating}"
        ) { backStackEntry ->

            // Parse data from route arguments
            val pid = backStackEntry.arguments?.getString("pid")?.toIntOrNull() ?: 1
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
            val rating = backStackEntry.arguments?.getString("rating")?.toFloatOrNull() ?: 0f

            // Render detail page
            ProductDetailScreen(
                pid = pid,
                name = name,
                price = price,
                rating = rating,
                navController = navController
            )
        }

        // ------------------------ Wishlist (Lists tab) ------------------------
        composable(Routes.LISTS) {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // Generic wishlist route (no parameters)
        composable("wishlist") {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // Wishlist route with uid parameter (for notification jump)
        composable(
            route = "wishlist/{uid}",
            arguments = listOf(
                navArgument("uid") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getInt("uid") ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // =============  Profile Screens =============
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(navController = navController)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController)
        }
        composable("history") {
            HistoryScreen(navController = navController)
        }

    }

}