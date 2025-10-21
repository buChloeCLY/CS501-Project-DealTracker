@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlin.math.roundToInt

enum class Platform { Amazon, BestBuy }

data class ProductUi(
    val title: String,
    val price: Double,
    val rating: Float,
    val source: String,
    val sales: Int,
    val platform: Platform,
    val freeShipping: Boolean,
    val inStock: Boolean
) {
    val priceText: String get() = "$" + "%.2f".format(price)
}

enum class SortField(val label: String) { Sales("Sales"), Price("Price"), Rating("Rating") }
enum class SortOrder { Asc, Desc }

@Composable
fun DealsScreen(
    navController: NavController,
    onCompareClick: (ProductUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val allProducts = remember {
        listOf(
            ProductUi("iPhone 16 Pro", 1099.0, 4.8f, "Apple Store", 200341, Platform.Amazon, true, true),
            ProductUi("Samsung S24 Ultra", 999.0, 4.6f, "Samsung Official", 150123, Platform.Amazon, true, true),
            ProductUi("Google Pixel 9", 849.0, 4.5f, "Google Store", 90123, Platform.BestBuy, true, true),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals") },
                actions = { IconButton(onClick = {}) { Icon(Icons.Filled.Search, null) } }
            )
        },
        bottomBar = { DealsBottomBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allProducts) { product ->
                ProductCard(
                    product = product,
                    onCompareClick = { onCompareClick(product) }
                )
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductUi, onCompareClick: (ProductUi) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Image, null) }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.priceText)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onCompareClick(product) }) {
                    Text("COMPARE")
                }
            }
        }
    }
}

@Composable
fun DealsBottomBar() {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Outlined.LocalOffer, null) }, label = { Text("Deals") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.Add, null) }, label = { Text("Lists") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Profile") })
    }
}

@Preview(showBackground = true)
@Composable
private fun DealsPreview() {
    val nav = rememberNavController()
    DealsScreen(nav, {})
}
