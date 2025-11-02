package com.example.dealtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dealtracker.ui.deals.DealsScreen
import com.example.dealtracker.ui.detail.ProductDetailScreen
import com.example.dealtracker.ui.home.HomeScreen

// ✅ 主导航图配置
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
        // 首页
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }

        // 商品列表页
        composable(Routes.DEALS) {
            DealsScreen(
                showBack = navController.previousBackStackEntry != null,
                onBack = { navController.popBackStack() },
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

        // 商品详情页（带参数）
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

        // TODO: 后续添加其他页面
        // composable(Routes.LISTS) { WishlistScreen(navController) }
        // composable(Routes.PROFILE) { ProfileScreen(navController) }
    }
}