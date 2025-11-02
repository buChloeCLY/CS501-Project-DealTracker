package com.example.dealtracker.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

// ✅ 导航扩展函数
fun NavHostController.navigateToRoot(route: String) {
    val target = route.substringBefore("?")   // 防止带参数的路由干扰

    // 先尝试直接从返回栈回到该目的地
    val popped = popBackStack(target, inclusive = false)
    if (!popped) {
        // 栈里没有，就用推荐写法导航到顶层目的地（保留/恢复状态）
        navigate(target) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}