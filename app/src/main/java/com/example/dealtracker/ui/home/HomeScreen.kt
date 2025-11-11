package com.example.dealtracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.dealtracker.ui.navigation.Routes
import com.example.dealtracker.ui.navigation.navigateToRoot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchMode) {
                        Text("Home", fontWeight = FontWeight.Bold)
                    } else {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search products...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isSearchMode && searchQuery.isNotEmpty()) {
                            navController.navigateToRoot(Routes.DEALS)
                        } else {
                            isSearchMode = !isSearchMode
                            if (!isSearchMode) searchQuery = ""
                        }
                    }) {
                        Icon(
                            imageVector = if (!isSearchMode) Icons.Default.Search else Icons.Default.Close,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            item {
                CategorySection(
                    onCategoryClick = { navController.navigateToRoot(Routes.DEALS) }
                )
            }
            item { DealsOfTheDaySection(navController = navController) }
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
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        for (row in displayedCategories.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
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
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp)
                    .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (expanded) "Less ▲" else "More ▼",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
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
        Text(
            text = category,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
    }
}

//  DealsOfTheDaySection 可点击 iPhone16 进入详情页
@Composable
fun DealsOfTheDaySection(navController: NavHostController) {
    Text(
        text = "Deals of the Day",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    val deals = listOf(
        Triple("iPhone 16", "$999", "Amazon"),
        Triple("Dyson Hair Dryer", "$399", "BestBuy"),
        Triple("Sony Headphones", "$249", "Walmart"),
        Triple("Nike Running Shoes", "$120", "Nike"),
        Triple("Apple Watch", "$349", "Target"),
        Triple("Samsung TV 65", "$799", "BestBuy"),
        Triple("MacBook Air M3", "$1199", "Apple"),
    )

    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        deals.forEach { (name, price, site) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp)
                    .clickable {
                        if (name == "iPhone 16") {
                            navController.navigate(
                                Routes.detailRoute(
                                    pid = 1,
                                    name = name,
                                    price = price.removePrefix("$").toDouble(),
                                    rating = 4.8f
                                )
                            )
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFDDEEE0), RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(price, color = Color(0xFF388E3C), fontSize = 15.sp)
                    Text(
                        "Available on $site",
                        color = Color.Gray,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
