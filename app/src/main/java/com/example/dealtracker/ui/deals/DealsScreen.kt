@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.dealtracker.ui.deals
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.deals.viewmodel.DealsViewModel
import com.example.dealtracker.ui.deals.viewmodel.SortField
import com.example.dealtracker.ui.deals.viewmodel.SortOrder
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.dealtracker.ui.navigation.Routes

// ---------------------- Top-level screen ----------------------

@Composable
fun DealsScreen(
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onCompareClick: (Product) -> Unit = {},  // ✅ 现在接收统一的 Product
    viewModel: DealsViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()

    // Filter & Sort sheet states
    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    // List state & coroutine scope
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                // Filter / Sort chips
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AssistChip(
                        onClick = { filterSheetOpen = true },
                        label = { Text("Filter") }
                    )
                    AssistChip(
                        onClick = { sortSheetOpen = true },
                        label = { Text("Sort") }
                    )
                }

                // Product list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    state = listState
                ) {
                    items(ui.filteredSorted) { product ->
                        ProductCard(
                            product = product,
                            onCompareClick = onCompareClick
                        )
                    }
                }
            }

            // Scroll to top FAB
            val showScrollTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 4 }
            }
            AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } }
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Back to top")
                }
            }
        }
    }

    // Filter Sheet
    if (filterSheetOpen) {
        FilterSheet(
            priceMin = ui.filters.priceMin,
            priceMax = ui.filters.priceMax,
            onPriceChange = { min, max -> viewModel.setPrice(min, max) },
            chooseAmazon = ui.filters.chooseAmazon,
            chooseBestBuy = ui.filters.chooseBestBuy,
            chooseWalmart = ui.filters.chooseWalmart,
            onPlatformToggle = { p, checked ->
                when (p) {
                    Platform.Amazon -> viewModel.toggleAmazon(checked)
                    Platform.BestBuy -> viewModel.toggleBestBuy(checked)
                    Platform.Walmart -> viewModel.toggleWalmart(checked)
                }
            },
            onlyFreeShipping = ui.filters.onlyFreeShipping,
            onOnlyFreeShippingChange = { viewModel.setOnlyFreeShipping(it) },
            onlyInStock = ui.filters.onlyInStock,
            onOnlyInStockChange = { viewModel.setOnlyInStock(it) },
            onClear = { viewModel.clearFilters() },
            onApply = { filterSheetOpen = false },
            onDismiss = { filterSheetOpen = false }
        )
    }

    // Sort Sheet
    if (sortSheetOpen) {
        SortSheet(
            sortField = ui.sort.field,
            sortOrder = ui.sort.order,
            onFieldChange = { viewModel.setSortField(it) },
            onOrderChange = { viewModel.setSortOrder(it) },
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
    chooseWalmart: Boolean,
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
            Text("Price Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

            // Platform
            Text("Platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                FilterChip(
                    selected = chooseWalmart,
                    onClick = { onPlatformToggle(Platform.Walmart, !chooseWalmart) },
                    label = { Text("Walmart") }
                )
            }
            Spacer(Modifier.height(16.dp))

            // Free shipping / Stock
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Free Shipping", style = MaterialTheme.typography.titleMedium)
                Switch(checked = onlyFreeShipping, onCheckedChange = onOnlyFreeShippingChange)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("In Stock", style = MaterialTheme.typography.titleMedium)
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

            // Field selection
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
                    HorizontalDivider()
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
    product: Product,  // ✅ 使用统一的 Product
    onCompareClick: (Product) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            // Product image placeholder
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

                // ✅ 只显示来源信息（由 platform 自动生成）
                Text(
                    product.sourceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onCompareClick(product) },
                    modifier = Modifier.widthIn(min = 120.dp)
                ) {
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
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ---------------------- Preview ----------------------

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun DealsScreenPreview() {
    val navController = rememberNavController()

    MaterialTheme {
        DealsScreen(
            onCompareClick = { product ->
                navController.navigate(
                    Routes.detailRoute(
                        pid = product.pid,
                        name = product.title,
                        price = product.price,
                        rating = product.rating
                    )
                )
            }
        )
    }
}