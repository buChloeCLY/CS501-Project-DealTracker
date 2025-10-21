@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.project

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.navigation.compose.rememberNavController


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

// ---------------------- ViewModel + StateFlow ----------------------

data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseBestBuy: Boolean = true,
    val onlyFreeShipping: Boolean = false,
    val onlyInStock: Boolean = false
)

data class DealsSortState(
    val field: SortField = SortField.Sales,
    val order: SortOrder = SortOrder.Desc
)

data class DealsUiState(
    val products: List<ProductUi> = emptyList(),
    val filteredSorted: List<ProductUi> = emptyList(),
    val filters: DealsFilterState = DealsFilterState(),
    val sort: DealsSortState = DealsSortState()
)

class DealsViewModel : ViewModel() {

    // 商品样例数据（来自你原文件）
    private val _products = MutableStateFlow(
        listOf(
            ProductUi("iPhone 16 Pro", 999.0, 4.6f, "Best Price from Amazon", 12030, Platform.Amazon,  true,  true),
            ProductUi("Samsung Galaxy Ultra", 999.0, 4.4f, "Best Price from Amazon", 10112, Platform.Amazon,  false, true),
            ProductUi("OnePlus 12", 799.0, 4.2f, "Official Store",           6120, Platform.BestBuy, true,  true),
            ProductUi("Google Pixel 9", 899.0, 4.5f, "Official Store",        8540, Platform.BestBuy, false, false),
            ProductUi("Moto X Pro", 699.0, 3.9f, "Best Price from Amazon",    2350, Platform.Amazon,  true,  true),
            // 额外样例
            ProductUi("Sony WH-1000XM5", 329.0, 4.7f, "Official Store",        9300, Platform.BestBuy, true,  true),
            ProductUi("AirPods Pro 2", 249.0, 4.6f, "Best Price from Amazon", 15230, Platform.Amazon,  true,  true),
            ProductUi("Nintendo Switch OLED", 349.0, 4.8f, "Official Store",  20110, Platform.BestBuy, false, true),
            ProductUi("Kindle Paperwhite", 139.0, 4.5f, "Best Price from Amazon", 18450, Platform.Amazon, true, true),
            ProductUi("GoPro HERO12", 399.0, 4.4f, "Official Store",           4210, Platform.BestBuy, false, true),
            ProductUi("Logitech MX Master 3S", 99.0, 4.7f, "Best Price from Amazon", 9750, Platform.Amazon, true, true),
        )
    )
    val products: StateFlow<List<ProductUi>> = _products

    private val _filters = MutableStateFlow(DealsFilterState())
    val filters: StateFlow<DealsFilterState> = _filters

    private val _sort = MutableStateFlow(DealsSortState())
    val sort: StateFlow<DealsSortState> = _sort

    val uiState: StateFlow<DealsUiState> =
        combine(products, filters, sort) { list, f, s ->
            val filtered = list.asSequence()
                .filter { it.price in f.priceMin..f.priceMax }
                .filter { (f.chooseAmazon && it.platform == Platform.Amazon) || (f.chooseBestBuy && it.platform == Platform.BestBuy) }
                .filter { if (f.onlyFreeShipping) it.freeShipping else true }
                .filter { if (f.onlyInStock) it.inStock else true }
                .toList()

            val sorted = when (s.field) {
                SortField.Sales  -> filtered.sortedBy { it.sales }
                SortField.Price  -> filtered.sortedBy { it.price }
                SortField.Rating -> filtered.sortedBy { it.rating }
            }.let { if (s.order == SortOrder.Desc) it.reversed() else it }

            DealsUiState(
                products = list,
                filteredSorted = sorted,
                filters = f,
                sort = s
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DealsUiState(products = _products.value, filteredSorted = _products.value) // 初始值
        )

    // ---- 更新方法，供 UI 调用 ----
    fun setPrice(min: Float, max: Float) = _filters.update { it.copy(priceMin = min, priceMax = max) }
    fun toggleAmazon(checked: Boolean)  = _filters.update { it.copy(chooseAmazon = checked) }
    fun toggleBestBuy(checked: Boolean) = _filters.update { it.copy(chooseBestBuy = checked) }
    fun setOnlyFreeShipping(v: Boolean) = _filters.update { it.copy(onlyFreeShipping = v) }
    fun setOnlyInStock(v: Boolean)      = _filters.update { it.copy(onlyInStock = v) }
    fun setSortField(field: SortField)   = _sort.update { it.copy(field = field) }
    fun setSortOrder(order: SortOrder)   = _sort.update { it.copy(order = order) }
    fun clearFilters()                   = _filters.update { DealsFilterState() }
}

// ---------------------- Top-level screen ----------------------

@Composable
fun DealsScreen(
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onCompareClick: (ProductUi) -> Unit = {},
    viewModel: DealsViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()

    // Filter states
    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    // 列表状态 + 协程作用域
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
        },
        bottomBar = {}
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)     // 👈 确保给到系统/底栏的安全区，按钮不会压住底栏
                .fillMaxSize()
        ) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    state = listState
                ) {
                    items(ui.filteredSorted) { item ->
                        ProductCard(product = item, onCompareClick = onCompareClick)
                    }
                }
            }
            val showScrollTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 4 }  // 5个以后出现
            }
            AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)  // 👈 与底栏错开，不遮挡
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } }
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Back to top")
                }
            }
        }

    }

    // Filter Sheet（读 UI 状态，写 ViewModel）
    if (filterSheetOpen) {
        FilterSheet(
            priceMin = ui.filters.priceMin,
            priceMax = ui.filters.priceMax,
            onPriceChange = { min, max -> viewModel.setPrice(min, max) },
            chooseAmazon = ui.filters.chooseAmazon,
            chooseBestBuy = ui.filters.chooseBestBuy,
            onPlatformToggle = { p, checked ->
                when (p) {
                    Platform.Amazon -> viewModel.toggleAmazon(checked)
                    Platform.BestBuy -> viewModel.toggleBestBuy(checked)
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

    // Sort Sheet（读 UI 状态，写 ViewModel）
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

// ---------------------- Preview ----------------------

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun DealsScreenPreview() {
    val navController = rememberNavController()   // ✅ 增加这行

    MaterialTheme {
        DealsScreen(
            onCompareClick = { product ->
                navController.navigate(
                    Routes.detailRoute(
                        name = product.title,
                        price = product.price,
                        rating = product.rating,
                        source = product.source
                    )
                )
            }
        )
    }
}
