package com.example.project

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.project.HomeScreen
import com.example.project.DealsScreen

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
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(Routes.DEALS) {
            DealsScreen()
        }
    }
}


