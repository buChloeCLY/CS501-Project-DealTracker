package com.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO: implement search */ }) {
                        Icon(Icons.Default.Person, contentDescription = "Search")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Routes.HOME,
                onHomeClick = { /* Already here */ },
                onDealsClick = { navController.navigate(Routes.DEALS) },
                onListClick = { /* TODO */ },
                onProfileClick = { /* TODO */ }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CategorySection(onCategoryClick = { navController.navigate(Routes.DEALS) }) }
            item { DealsOfTheDaySection() }
        }
    }
}

@Composable
fun CategorySection(onCategoryClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val allCategories = listOf(
        "Electronics", "Beauty", "Home", "Food", "Fashion", "Sports",
        "Books", "Toys", "Health", "Outdoors", "Office", "Pets"
    )

    val displayedCategories = if (expanded) allCategories else allCategories.take(6)

    Column {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        for (rowIndex in displayedCategories.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowIndex.forEach { category ->
                    CategoryCard(
                        category = category,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick() }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp)
                    .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text(if (expanded) "Less ▲" else "More ▼", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CategoryCard(category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .background(Color(0xFFF2F2F2), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(category, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DealsOfTheDaySection() {
    Text(
        text = "Deals of the Day",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    val deals = listOf(
        Triple("iPhone 16 Pro", "$999", "Amazon"),
        Triple("Dyson Hair Dryer", "$399", "BestBuy"),
        Triple("Sony Headphones", "$249", "Walmart"),
        Triple("Nike Running Shoes", "$120", "Nike"),
        Triple("Apple Watch", "$349", "Target"),
        Triple("Samsung TV 65\"", "$799", "BestBuy"),
        Triple("MacBook Air M3", "$1199", "Apple"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        deals.forEach { (name, price, site) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFDDEEE0), RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.SemiBold)
                    Text(price, color = Color(0xFF388E3C))
                    Text("Available on $site", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onDealsClick: () -> Unit,
    onListClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = onHomeClick,
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.DEALS,
            onClick = onDealsClick,
            icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = "Deals") },
            label = { Text("Deals") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.LISTS,
            onClick = onListClick,
            icon = { Icon(Icons.Outlined.Add, contentDescription = "Lists") },
            label = { Text("Lists") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = onProfileClick,
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
