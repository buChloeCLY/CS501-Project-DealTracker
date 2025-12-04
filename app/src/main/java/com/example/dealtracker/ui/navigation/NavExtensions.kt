package com.example.dealtracker.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * 简化后的 navigateToRoot
 * - 保留 route（包含 query 参数）
 * - 不重用旧页面，不恢复状态
 * - 保证每次都触发搜索刷新
 */
fun NavHostController.navigateToRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
        }
        launchSingleTop = true     // 避免堆栈重复
    }
}
