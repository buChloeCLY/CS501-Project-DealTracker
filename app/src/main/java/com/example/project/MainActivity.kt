package com.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

            Scaffold(
                bottomBar = {
                    BottomNavBarRouteAware(
                        currentRoute = currentRoute,
                        onTabSelected = { to ->
                            navController.navigateToRoot(to)
                        }
                    )
                }
            ) { inner ->
                // 其它页面都在 NavHost 中切换；底栏固定不动
                MainNavGraph(navController, modifier = Modifier.padding(inner))
            }
        }
    }
}

@Composable
fun BottomNavBarRouteAware(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onTabSelected(Routes.HOME) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.DEALS,
            onClick = { onTabSelected(Routes.DEALS) },
            icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = "Deals") },
            label = { Text("Deals") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.LISTS,
            onClick = { onTabSelected(Routes.LISTS) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = "Lists") },
            label = { Text("Lists") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = { onTabSelected(Routes.PROFILE) },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

fun NavHostController.navigateToRoot(route: String) {
    val target = route.substringBefore("?")   // 防止带参数的路由干扰
    // 先尝试直接从返回栈回到该目的地
    val popped = popBackStack(target, inclusive = false)
    if (!popped) {
        // 栈里没有，就用推荐写法导航到顶层目的地（保留/恢复状态）
        navigate(target) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
