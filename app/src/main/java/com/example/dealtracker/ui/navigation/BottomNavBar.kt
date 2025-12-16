package com.example.dealtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.dealtracker.ui.theme.AppTheme

@Composable
fun BottomNavBarRouteAware(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    val colors = AppTheme.colors

    NavigationBar(
        containerColor = colors.bottomBarBackground
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.bottomBarSelected,
            selectedTextColor = colors.bottomBarSelected,
            unselectedIconColor = colors.bottomBarUnselected,
            unselectedTextColor = colors.bottomBarUnselected,
            indicatorColor = colors.bottomBarSelected.copy(alpha = 0.16f)
        )

        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onTabSelected(Routes.HOME) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentRoute == Routes.DEALS,
            onClick = {
                // Always navigate to the default Deals screen from bottom tab
                onTabSelected(Routes.DEALS)
            },
            icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = "Deals") },
            label = { Text("Deals") },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentRoute == Routes.LISTS,
            onClick = { onTabSelected(Routes.LISTS) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = "Wishlists") },
            label = { Text("Wishlists") },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = { onTabSelected(Routes.PROFILE) },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = itemColors
        )
    }
}
