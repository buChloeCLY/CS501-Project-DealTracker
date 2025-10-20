package com.example.project

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

//管理页面之间的导航跳转：
//“从哪一个界面跳到哪一个界面”
//“跳转时要带哪些参数”
//“按返回键该回到哪里”
object Routes {
    const val DEALS = "deals"
    const val DETAIL_BASE = "detail"
    // 使用 Query 参数避免路径编码问题
    fun detailRoute(name: String, price: String, rating: Float, source: String): String {
        val n = Uri.encode(name)
        val p = Uri.encode(price)
        val s = Uri.encode(source)
        return "$DETAIL_BASE?name=$n&price=$p&rating=$rating&source=$s"
    }
}

@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.DEALS
    ) {
        // 列表页
        composable(Routes.DEALS) {
            DealsScreen(
                onCompareClick = { product: ProductUi ->
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

        // 其他页面……
    }
}
