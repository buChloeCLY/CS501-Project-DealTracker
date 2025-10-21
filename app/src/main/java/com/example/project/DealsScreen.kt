@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import kotlin.math.roundToInt

// ---------------------- Models ----------------------

enum class Platform { Amazon, BestBuy }

data class ProductUi(
    val title: String,
    val price: Double,
    val rating: Float,     // 0..5
    val source: String,    // "Best Price from Amazon" ...
    val sales: Int,        // 用于“销量”排序
    val platform: Platform,
    val freeShipping: Boolean,
    val inStock: Boolean
) {
    val priceText: String get() = "$" + "%.2f".format(price)
}

enum class SortField(val label: String) {
    Sales("Sales"),
    Price("Price"),
    Rating("Rating")
}
enum class SortOrder { Asc, Desc }

// ---------------------- Top-level screen ----------------------

@Composable
fun DealsScreen(
    modifier: Modifier = Modifier,
    onCompareClick: (ProductUi) -> Unit = {}
) {
    // Mock data
    val allProducts = remember {
        listOf(
            ProductUi("iPhone 16 Pro", 999.0, 4.6f, "Best Price from Amazon", sales = 12030, platform = Platform.Amazon, freeShipping = true,  inStock = true),
            ProductUi("Samsung Galaxy Ultra", 999.0, 4.4f, "Best Price from Amazon", sales = 10112, platform = Platform.Amazon, freeShipping = false, inStock = true),
            ProductUi("OnePlus 12", 799.0, 4.2f, "Official Store",         sales =  6120, platform = Platform.BestBuy, freeShipping = true,  inStock = true),
            ProductUi("Google Pixel 9", 899.0, 4.5f, "Official Store",     sales =  8540, platform = Platform.BestBuy, freeShipping = false, inStock = false),
            ProductUi("Moto X Pro", 699.0, 3.9f, "Best Price from Amazon", sales =  2350, platform = Platform.Amazon,  freeShipping = true,  inStock = true),
        )
    }

    // Filter states
    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    // price range（为简单起见 0~2000）
    var priceMin by remember { mutableStateOf(0f) }
    var priceMax by remember { mutableStateOf(2000f) }

    // platform 精选
    var chooseAmazon by remember { mutableStateOf(true) }
    var chooseBestBuy by remember { mutableStateOf(true) }

    // free shipping / stock availability
    var onlyFreeShipping by remember { mutableStateOf(false) }
    var onlyInStock by remember { mutableStateOf(false) }

    // Sort
    var sortField by remember { mutableStateOf(SortField.Sales) }
    var sortOrder by remember { mutableStateOf(SortOrder.Desc) } // 默认“从高到低”

    // Derived list after filter + sort
    val filteredSorted = remember(
        allProducts, priceMin, priceMax, chooseAmazon, chooseBestBuy, onlyFreeShipping, onlyInStock, sortField, sortOrder
    ) {
        allProducts
            .asSequence()
            .filter { it.price in priceMin..priceMax }
            .filter { (chooseAmazon && it.platform == Platform.Amazon) || (chooseBestBuy && it.platform == Platform.BestBuy) }
            .filter { if (onlyFreeShipping) it.freeShipping else true }
            .filter { if (onlyInStock) it.inStock else true }
            .sortedWith(
                when (sortField) {
                    SortField.Sales -> compareBy<ProductUi> { it.sales }
                    SortField.Price -> compareBy { it.price }
                    SortField.Rating -> compareBy { it.rating }
                }.let { cmp -> if (sortOrder == SortOrder.Desc) cmp.reversed() else cmp }
            )
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { /* TODO search */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
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
            // Filter / Sort entry
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(onClick = { filterSheetOpen = true }, label = { Text("filter") })
                AssistChip(onClick = { sortSheetOpen = true }, label = { Text("sort") })
            }

            // List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredSorted) { item ->
                    ProductCard(product = item, onCompareClick = onCompareClick)
                }
            }
        }
    }

    if (filterSheetOpen) {
        FilterSheet(
            priceMin = priceMin,
            priceMax = priceMax,
            onPriceChange = { min, max -> priceMin = min; priceMax = max },
            chooseAmazon = chooseAmazon,
            chooseBestBuy = chooseBestBuy,
            onPlatformToggle = { p, checked ->
                when (p) {
                    Platform.Amazon -> chooseAmazon = checked
                    Platform.BestBuy -> chooseBestBuy = checked
                }
            },
            onlyFreeShipping = onlyFreeShipping,
            onOnlyFreeShippingChange = { onlyFreeShipping = it },
            onlyInStock = onlyInStock,
            onOnlyInStockChange = { onlyInStock = it },
            onClear = {
                priceMin = 0f; priceMax = 2000f
                chooseAmazon = true; chooseBestBuy = true
                onlyFreeShipping = false; onlyInStock = false
            },
            onApply = { filterSheetOpen = false },
            onDismiss = { filterSheetOpen = false }
        )
    }

    if (sortSheetOpen) {
        SortSheet(
            sortField = sortField,
            sortOrder = sortOrder,
            onFieldChange = { sortField = it },
            onOrderChange = { sortOrder = it },
            onDismiss = { sortSheetOpen = false }
        )
    }
}

// ---------------------- Filter Sheet ----------------------

@Composable
private fun FilterSheet(
    priceMin: Float,
    priceMax: Float,
    onPriceChange: (Float, Float) -> Unit,
    chooseAmazon: Boolean,
    chooseBestBuy: Boolean,
    onPlatformToggle: (Platform, Boolean) -> Unit,
    onlyFreeShipping: Boolean,
    onOnlyFreeShippingChange: (Boolean) -> Unit,
    onlyInStock: Boolean,
    onOnlyInStockChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Filter", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            // Price Range
            Text("price range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            var tmpRange by remember { mutableStateOf(priceMin..priceMax) }
            Text(
                "$${tmpRange.start.roundToInt()} - $${tmpRange.endInclusive.roundToInt()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            RangeSlider(
                value = tmpRange,
                onValueChange = { range ->
                    val start = range.start.coerceIn(0f, 2000f)
                    val end = range.endInclusive.coerceIn(0f, 2000f)
                    tmpRange = start..end
                },
                valueRange = 0f..2000f,
                steps = 19
            )

            Spacer(Modifier.height(16.dp))

            // Platform 精选
            Text("platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chooseAmazon,
                    onClick = { onPlatformToggle(Platform.Amazon, !chooseAmazon) },
                    label = { Text("Amazon") }
                )
                FilterChip(
                    selected = chooseBestBuy,
                    onClick = { onPlatformToggle(Platform.BestBuy, !chooseBestBuy) },
                    label = { Text("BestBuy") }
                )
            }
            Spacer(Modifier.height(16.dp))

            // Free shipping / Stock availability
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("free shipping", style = MaterialTheme.typography.titleMedium)
                Switch(checked = onlyFreeShipping, onCheckedChange = onOnlyFreeShippingChange)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("stock availability", style = MaterialTheme.typography.titleMedium)
                Switch(checked = onlyInStock, onCheckedChange = onOnlyInStockChange)
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClear) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onPriceChange(tmpRange.start, tmpRange.endInclusive)
                    onApply()
                }) { Text("Apply") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------- Sort Sheet ----------------------

@Composable
private fun SortSheet(
    sortField: SortField,
    sortOrder: SortOrder,
    onFieldChange: (SortField) -> Unit,
    onOrderChange: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Sort", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose a field and order",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Field
            Column {
                SortField.values().forEach { f ->
                    ListItem(
                        headlineContent = { Text(f.label) },
                        trailingContent = {
                            RadioButton(
                                selected = (sortField == f),
                                onClick = { onFieldChange(f) }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Order", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = (sortOrder == SortOrder.Asc),
                    onClick = { onOrderChange(SortOrder.Asc) },
                    label = { Text("Low to High") },
                    leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) }
                )
                FilterChip(
                    selected = (sortOrder == SortOrder.Desc),
                    onClick = { onOrderChange(SortOrder.Desc) },
                    label = { Text("High to Low") },
                    leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss) { Text("Done") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------- Product Card ----------------------

@Composable
private fun ProductCard(
    product: ProductUi,
    onCompareClick: (ProductUi) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDEEE0)),
                contentAlignment = Alignment.Center
            ) {
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

                Spacer(Modifier.height(6.dp))
                Text(product.priceText, style = MaterialTheme.typography.titleSmall)
                val sub = when (product.platform) {
                    Platform.Amazon -> "Amazon"
                    Platform.BestBuy -> "BestBuy"
                }
                Text(
                    "${product.source} • $sub • Sales ${product.sales}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))
                Button(onClick = { onCompareClick(product) }, modifier = Modifier.widthIn(min = 120.dp)) {
                    Text("COMPARE")
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
            val iv = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder
            Icon(
                imageVector = iv,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ---------------------- Bottom Bar ----------------------

@Composable
private fun DealsBottomBar() {
    NavigationBar {
        NavigationBarItem(
            selected = true, onClick = { /* TODO */ },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false, onClick = { /* TODO */ },
            icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = "Deals") },
            label = { Text("Deals") }
        )
        NavigationBarItem(
            selected = false, onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.Add, contentDescription = "Lists") },
            label = { Text("Lists") }
        )
        NavigationBarItem(
            selected = false, onClick = { /* TODO */ },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

// ---------------------- Preview ----------------------

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun DealsScreenPreview() {
    MaterialTheme { DealsScreen() }
}
