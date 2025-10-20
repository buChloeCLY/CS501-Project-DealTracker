@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ---------------------- Data models ----------------------

enum class FilterOption(val label: String) {
    PriceRange("price range"),
    Platform("platform"),
    FreeShipping("free shipping"),
    SellerType("seller type"),
    StockAvailability("stock availability")
}

// 排序方式（英文翻译）：
// 销量 -> Sales
// 价格从低到高 -> Price: Low to High
// 价格从高到低 -> Price: High to Low
// 评分 -> Rating
enum class SortOption(val label: String) {
    Sales("Sales"),
    PriceLowToHigh("Price: Low to High"),
    PriceHighToLow("Price: High to Low"),
    Rating("Rating")
}

data class ProductUi(
    val title: String,
    val price: String,
    val rating: Float,   // 0..5
    val source: String,  // e.g., "Best Price from Amazon"
    val badgeText: String = "COMPARE"
)

// ---------------------- Top-level screen ----------------------

@Composable
fun DealsScreen(
    modifier: Modifier = Modifier,
    onCompareClick: (ProductUi) -> Unit = {}
) {
    val products = remember {
        listOf(
            ProductUi("iPhone 16 Pro", "$999", 4.0f, "Best Price from Amazon"),
            ProductUi("Samsung Ultra", "$999", 4.3f, "Best Price from Amazon"),
            ProductUi("OnePlus 12", "$799", 3.5f, "Official Store")
        )
    }

    var openFilter by remember { mutableStateOf(false) }
    var openSort by remember { mutableStateOf(false) }

    var selectedFilters by remember { mutableStateOf(setOf<FilterOption>()) }
    var selectedSort by remember { mutableStateOf(SortOption.Sales) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { /* TODO: search action */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        },
        bottomBar = { DealsBottomBar() }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Filter / Sort row (chips)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = { openFilter = true },
                    label = { Text("filter") }
                )
                AssistChip(
                    onClick = { openSort = true },
                    label = { Text("sort") }
                )
            }

            // Product list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { item ->
                    ProductCard(
                        product = item,
                        onCompareClick = onCompareClick
                    )
                }
            }
        }
    }

    // Filter sheet
    if (openFilter) {
        ModalBottomSheet(onDismissRequest = { openFilter = false }) {
            Text(
                "Filter",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            FlowRowWithChips(
                options = FilterOption.values().toList(),
                selected = selectedFilters,
                onToggle = { opt ->
                    selectedFilters =
                        if (opt in selectedFilters) selectedFilters - opt else selectedFilters + opt
                }
            )
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { selectedFilters = emptySet() }) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { openFilter = false }) { Text("Apply") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // Sort sheet
    if (openSort) {
        ModalBottomSheet(onDismissRequest = { openSort = false }) {
            Text(
                "Sort",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Column(Modifier.padding(horizontal = 8.dp)) {
                SortOption.values().forEach { opt ->
                    ListItem(
                        headlineContent = { Text(opt.label) },
                        trailingContent = {
                            RadioButton(
                                selected = selectedSort == opt,
                                onClick = { selectedSort = opt; openSort = false }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------------------- Reusable UI pieces ----------------------

@Composable
private fun ProductCard(
    product: ProductUi,
    onCompareClick: (ProductUi) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp)) {
            // Left: circular placeholder image
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDEEE0)),
                contentAlignment = Alignment.Center
            ) {
                // 用占位图标（你也可换成真实图片）
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    product.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                StarsRow(rating = product.rating)

                Spacer(Modifier.height(4.dp))
                Text(product.price, style = MaterialTheme.typography.titleSmall)
                Text(
                    product.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onCompareClick(product) },
                    modifier = Modifier.widthIn(min = 120.dp)
                ) {
                    Text(product.badgeText)
                }
            }
        }
    }
}

@Composable
private fun StarsRow(rating: Float, max: Int = 5) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { idx ->
            val filled = idx < rating.toInt()
            val imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DealsBottomBar() {
    NavigationBar {
        NavigationBarItem(
            selected = true, onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false, onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.List, contentDescription = "Deals") },
            label = { Text("Deals") }
        )
        NavigationBarItem(
            selected = false, onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Add, contentDescription = "Lists") },
            label = { Text("Lists") }
        )
        NavigationBarItem(
            selected = false, onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

// 简单 FlowRow（无依赖库）——用多行换行布局放筛选 Chip
@Composable
private fun FlowRowWithChips(
    options: List<FilterOption>,
    selected: Set<FilterOption>,
    onToggle: (FilterOption) -> Unit
) {
    // 简约实现：每行放3个
    Column(Modifier.padding(horizontal = 8.dp)) {
        options.chunked(3).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { opt ->
                    FilterChip(
                        selected = opt in selected,
                        onClick = { onToggle(opt) },
                        label = { Text(opt.label) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DealsScreenPreview() {
    MaterialTheme { DealsScreen() }
}
