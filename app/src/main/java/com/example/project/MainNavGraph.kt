package com.example.project

import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.page3.viewmodel.ProductDetailScreen

//管理页面之间的导航跳转：
//“从哪一个界面跳到哪一个界面”
//“跳转时要带哪些参数”
//“按返回键该回到哪里”

object Routes {
    const val HOME = "home"
    const val DEALS = "deals"
    const val LISTS = "lists"
    const val PROFILE = "profile"
    const val DETAIL_BASE = "detail"
    // 使用 Query 参数避免路径编码问题
    fun detailRoute(name: String, price: Double, rating: Float, source: String): String {
        val n = Uri.encode(name)
        val p = Uri.encode(price.toString())
        val s = Uri.encode(source)
        return "$DETAIL_BASE?name=$n&price=$p&rating=$rating&source=$s"
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
                            name = product.title,
                            price = product.price,
                            rating = product.rating,
                            source = product.source
                        )
                    )
                }
            )
        }

        composable(
            route = Routes.DETAIL_BASE +
                    "?name={name}&price={price}&rating={rating}&source={source}"
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull() ?: 0.0
            val rating = backStackEntry.arguments?.getString("rating")?.toFloatOrNull() ?: 0f
            val source = backStackEntry.arguments?.getString("source") ?: ""

            ProductDetailScreen(
                name = name,
                price = price,
                rating = rating,
                source = source,
                navController = navController
            )
        }

    }
}