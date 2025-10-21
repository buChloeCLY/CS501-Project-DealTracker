package com.example.project

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.project.ui.theme.ProductDetailScreen
import com.example.project.DealsScreen
import com.example.page3.model.Product

object Routes {
    const val DEALS = "deals"
    const val DETAIL_BASE = "detail"
    fun detailRoute(name: String, price: Double, rating: Float, source: String): String {
        val n = Uri.encode(name)
        val p = Uri.encode(price.toString())
        val r = Uri.encode(rating.toString())
        val s = Uri.encode(source)
        return "$DETAIL_BASE?name=$n&price=$p&rating=$r&source=$s"
    }
}

@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.DEALS
    ) {
        composable(Routes.DEALS) {
            DealsScreen(
                navController = navController,
                onCompareClick = { productUi ->
                    navController.navigate(
                        Routes.detailRoute(
                            name = productUi.title,
                            price = productUi.price,
                            rating = productUi.rating,
                            source = productUi.source
                        )
                    )
                }
            )
        }

        composable(
            route = "${Routes.DETAIL_BASE}?name={name}&price={price}&rating={rating}&source={source}"
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
