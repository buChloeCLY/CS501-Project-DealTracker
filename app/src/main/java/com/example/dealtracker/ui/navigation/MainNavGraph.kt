package com.example.dealtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dealtracker.ui.deals.DealsScreen
import com.example.dealtracker.ui.detail.ProductDetailScreen
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
 * 主导航图配置
 * 管理应用的页面跳转关系
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {

        // ------------------------ Home ------------------------
        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                //  在 HomeScreen 输入搜索词时跳转：
                // navController.navigate("deals?query=$query")
            )
        }

        // ------------------------ Deals（支持 query 参数） ------------------------
        composable(
            route = "deals?query={query}"
        ) { backStackEntry ->

            //  读取从 HomeScreen 传来的 search query
            val query = backStackEntry.arguments?.getString("query")

            DealsScreen(
                showBack = navController.previousBackStackEntry != null,
                onBack = { navController.popBackStack() },

                // 传到 DealsScreen 进行搜索分页加载
                searchQuery = query,

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

        // ------------------------ Product Detail ------------------------
        composable(
            route = Routes.DETAIL_BASE +
                    "?pid={pid}&name={name}&price={price}&rating={rating}"
        ) { backStackEntry ->

            val pid = backStackEntry.arguments?.getString("pid")?.toIntOrNull() ?: 1
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
            val rating = backStackEntry.arguments?.getString("rating")?.toFloatOrNull() ?: 0f

            ProductDetailScreen(
                pid = pid,
                name = name,
                price = price,
                rating = rating,
                navController = navController
            )
        }

        // ------------------------ Wishlist ------------------------
        composable(Routes.LISTS) {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        composable("wishlist") {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // ------------------------ Profile ------------------------
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
    }
}
