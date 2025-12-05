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
 *
 * 管理应用的页面跳转关系，基于 Navigation Compose。
 * 每个 composable() 定义一个路由对应的页面。
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // NavHost：设置起始页（Home）
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {

        // ============= 首页 Home =============
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }

// ============= Deals 页面（普通进入） =============
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

// ============= Deals 页面（分类进入） =============
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
                searchQuery = null, // 不走搜索
                category = category, // 分类专用参数
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


// ============= Deals 页面（搜索进入） =============
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
                category = null,  // 搜索时分类为空
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

        // ============= 商品详情页（带参数） =============
        composable(
            route = Routes.DETAIL_BASE +
                    "?pid={pid}&name={name}&price={price}&rating={rating}"
        ) { backStackEntry ->

            // 从路由参数中解析数据
            val pid = backStackEntry.arguments?.getString("pid")?.toIntOrNull() ?: 1
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
            val rating = backStackEntry.arguments?.getString("rating")?.toFloatOrNull() ?: 0f

            // 渲染详情页（含历史价格图表 + 平台信息）
            ProductDetailScreen(
                pid = pid,
                name = name,
                price = price,
                rating = rating,
                navController = navController
            )
        }

        // ============= 收藏页 WishList =============
        composable(Routes.LISTS) {
            // 从 UserManager 拿当前用户
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0  // 未登录时为 0，你在 WishListScreen 里可以根据 0 做跳转登录

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // 兼容 Profile 里使用的硬编码 "wishlist" 路由
        composable("wishlist") {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // =============  Profile 页面 =============
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
