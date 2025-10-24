package com.example.project

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.page3.viewmodel.ProductDetailScreen

object Routes {
    const val HOME = "home"
    const val DEALS = "deals"
    const val LISTS = "lists"
    const val PROFILE = "profile"
    const val DETAIL_BASE = "detail"

    fun detailRoute(pid: Int, name: String, price: Double, rating: Float, source: String): String {
        val n = Uri.encode(name)
        val p = Uri.encode(price.toString())
        val s = Uri.encode(source)
        return "$DETAIL_BASE?pid=$pid&name=$n&price=$p&rating=$rating&source=$s"
    }
}

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }

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
                            rating = product.rating,
                            source = product.source
                        )
                    )
                }
            )
        }

        // ✅ 这里加上 pid
        composable(
            route = Routes.DETAIL_BASE +
                    "?pid={pid}&name={name}&price={price}&rating={rating}&source={source}"
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getString("pid")?.toIntOrNull() ?: 1
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
            val rating = backStackEntry.arguments?.getString("rating")?.toFloatOrNull() ?: 0f
            val source = backStackEntry.arguments?.getString("source") ?: ""

            ProductDetailScreen(
                pid = pid,
                name = name,
                price = price,
                rating = rating,
                source = source,
                navController = navController
            )
        }
    }
}
