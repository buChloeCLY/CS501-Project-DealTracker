package com.example.project

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.page3.viewmodel.ProductDetailScreen
import com.example.page3.model.Product

object Routes {
    const val HOME = "home"
    const val DEALS = "deals"
    const val LISTS = "lists"
    const val PROFILE = "profile"
    const val DETAIL_BASE = "detail"

    fun detailRoute(pid: Int, name: String, price: Double, rating: Float): String {
        val n = Uri.encode(name)
        val p = Uri.encode(price.toString())
        return "$DETAIL_BASE?pid=$pid&name=$n&price=$p&rating=$rating"
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
                onCompareClick = { product: Product ->
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

        // ✅ 这里加上 pid
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
    }
}
