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

        // ============= ⭐ 商品详情页（仅 pid - 用于 Deep Link 和通知） =============
        composable(
            route = "detail/{pid}",
            arguments = listOf(
                navArgument("pid") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getInt("pid") ?: 1

            // ⭐ 使用新组件，会自动从 API 加载产品信息
            ProductDetailScreenWithPid(
                pid = pid,
                navController = navController
            )
        }

        // ============= 商品详情页（完整参数 - 向后兼容） =============
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

        // ------------------------ Wishlist（Lists 标签页） ------------------------
        composable(Routes.LISTS) {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // ⭐ 添加：通用 wishlist 路由（不带参数）
        composable("wishlist") {
            val currentUser by UserManager.currentUser.collectAsState()
            val uid = currentUser?.uid ?: 0

            WishListScreen(
                navController = navController,
                currentUserId = uid
            )
        }

        // ⭐ 添加：带 uid 参数的 wishlist 路由（用于通知点击跳转）
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