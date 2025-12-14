package com.example.dealtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// 底部导航栏组件
@Composable
fun BottomNavBarRouteAware(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onTabSelected(Routes.HOME) },
            icon = {
                Icon(
                    Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = currentRoute == Routes.DEALS,
            onClick = { onTabSelected(Routes.DEALS) },
            icon = {
                Icon(
                    Icons.Outlined.LocalOffer,
                    contentDescription = "Deals"
                )
            },
            label = { Text("Deals") }
        )

        NavigationBarItem(
            selected = currentRoute == Routes.LISTS,
            onClick = { onTabSelected(Routes.LISTS) },
            icon = {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Wishlists"
                )
            },
            label = { Text("Wishlists") }
        )

        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = { onTabSelected(Routes.PROFILE) },
            icon = {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") }
        )
    }
}