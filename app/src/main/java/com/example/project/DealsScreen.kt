@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.project

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.page3.model.Product
import com.example.page3.model.Platform
import com.example.page3.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------- 排序 ----------------------

private enum class SortField(val label: String) { Price("Price"), Rating("Rating") }
private enum class SortOrder { Asc, Desc }

// ---------------------- 过滤状态 ----------------------

private data class DealsFilterState(
    val priceMin: Float = 0f,
    val priceMax: Float = 2000f,
    val chooseAmazon: Boolean = true,
    val chooseBestBuy: Boolean = true,
    val onlyFreeShipping: Boolean = false,
    val onlyInStock: Boolean = false
)

private data class DealsSortState(
    val field: SortField = SortField.Price,
    val order: SortOrder = SortOrder.Asc
)

private data class DealsUiState(
    val products: List<Product> = emptyList(),
    val filteredSorted: List<Product> = emptyList(),
    val filters: DealsFilterState = DealsFilterState(),
    val sort: DealsSortState = DealsSortState()
)

// ---------------------- 页面级 UI ViewModel（只管 UI 状态） ----------------------

private class DealsUiViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    private val _filters = MutableStateFlow(DealsFilterState())
    private val _sort = MutableStateFlow(DealsSortState())

    val uiState: StateFlow<DealsUiState> =
        combine(_products, _filters, _sort) { list, f, s ->
            val filtered = list.asSequence()
                .filter { it.price.toFloat() in f.priceMin..f.priceMax }
                .filter {
                    (f.chooseAmazon && it.platform == Platform.Amazon) ||
                            (f.chooseBestBuy && it.platform == Platform.BestBuy)
                }
                .filter { if (f.onlyFreeShipping) it.freeShipping else true }
                .filter { if (f.onlyInStock) it.inStock else true }
                .toList()

            val sortedBase = when (s.field) {
                SortField.Price  -> filtered.sortedBy { it.price }
                SortField.Rating -> filtered.sortedBy { it.rating }
            }
            val sorted = if (s.order == SortOrder.Desc) sortedBase.reversed() else sortedBase

            DealsUiState(
                products = list,
                filteredSorted = sorted,
                filters = f,
                sort = s
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DealsUiState())

    fun setItems(newItems: List<Product>) = _products.update { newItems }

    // filter/sort 修改器
    fun setPrice(min: Float, max: Float) = _filters.update { it.copy(priceMin = min, priceMax = max) }
    fun toggleAmazon(checked: Boolean)  = _filters.update { it.copy(chooseAmazon = checked) }
    fun toggleBestBuy(checked: Boolean) = _filters.update { it.copy(chooseBestBuy = checked) }
    fun setOnlyFreeShipping(v: Boolean) = _filters.update { it.copy(onlyFreeShipping = v) }
    fun setOnlyInStock(v: Boolean)      = _filters.update { it.copy(onlyInStock = v) }
    fun setSortField(field: SortField)   = _sort.update { it.copy(field = field) }
    fun setSortOrder(order: SortOrder)   = _sort.update { it.copy(order = order) }
    fun clearFilters()                   = _filters.update { DealsFilterState() }
}

// ---------------------- Screen ----------------------

@Composable
fun DealsScreen(
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onCompareClick: (Product) -> Unit = {},
    productVm: ProductViewModel = viewModel(),
) {
    // 用你们 ProductViewModel 的样板构造临时列表；接入真实仓库时替换这里
    val template: Product = remember { productVm.getProduct() } // 读取示例 Product（来自你们现有 VM）
    val uiVm = viewModel<DealsUiViewModel>()

    LaunchedEffect(Unit) {
        val list = listOf(
            template.copy(pid = 1, title = "iPhone 16 Pro",  price = 999.0, rating = 4.6f, platform = Platform.Amazon,  freeShipping = true,  inStock = true),
            template.copy(pid = 2, title = "Samsung Galaxy Ultra", price = 999.0, rating = 4.4f, platform = Platform.Amazon,  freeShipping = false, inStock = true),
            template.copy(pid = 3, title = "OnePlus 12",      price = 799.0, rating = 4.2f, platform = Platform.BestBuy, freeShipping = true,  inStock = true),
            template.copy(pid = 4, title = "Google Pixel 9",  price = 899.0, rating = 4.5f, platform = Platform.BestBuy, freeShipping = false, inStock = false),
            template.copy(pid = 5, title = "Moto X Pro",      price = 699.0, rating = 3.9f, platform = Platform.Amazon,  freeShipping = true,  inStock = true),
            template.copy(pid = 6, title = "Sony WH-1000XM5", price = 329.0, rating = 4.7f, platform = Platform.BestBuy, freeShipping = true,  inStock = true),
            template.copy(pid = 7, title = "AirPods Pro 2",   price = 249.0, rating = 4.6f, platform = Platform.Amazon,  freeShipping = true,  inStock = true),
            template.copy(pid = 8, title = "Switch OLED",     price = 349.0, rating = 4.8f, platform = Platform.BestBuy, freeShipping = false, inStock = true),
            template.copy(pid = 9, title = "Kindle PW",       price = 139.0, rating = 4.5f, platform = Platform.Amazon,  freeShipping = true,  inStock = true),
            template.copy(pid =10, title = "GoPro HERO12",    price = 399.0, rating = 4.4f, platform = Platform.BestBuy, freeShipping = false, inStock = true),
        )
        uiVm.setItems(list)
    }

    val ui by uiVm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AssistChip(onClick = { filterSheetOpen = true }, label = { Text("filter") })
                    AssistChip(onClick = { sortSheetOpen = true }, label = { Text("sort") })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    state = listState
                ) {
                    items(ui.filteredSorted) { product ->
                        ProductCard(
                            product = product,
                            onCompareClick = { onCompareClick(product) }
                        )
                    }
                }
            }

            val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
            AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } }
                ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Back to top") }
            }
        }
    }

    if (filterSheetOpen) {
        FilterSheet(
            priceMin = ui.filters.priceMin,
            priceMax = ui.filters.priceMax,
            onPriceChange = { min, max -> uiVm.setPrice(min, max) },
            chooseAmazon = ui.filters.chooseAmazon,
            chooseBestBuy = ui.filters.chooseBestBuy,
            onPlatformToggle = { p, checked ->
                when (p) {
                    Platform.Amazon -> uiVm.toggleAmazon(checked)
                    Platform.BestBuy -> uiVm.toggleBestBuy(checked)
                }
            },
            onlyFreeShipping = ui.filters.onlyFreeShipping,
            onOnlyFreeShippingChange = { uiVm.setOnlyFreeShipping(it) },
            onlyInStock = ui.filters.onlyInStock,
            onOnlyInStockChange = { uiVm.setOnlyInStock(it) },
            onClear = { uiVm.clearFilters() },
            onApply = { filterSheetOpen = false },
            onDismiss = { filterSheetOpen = false }
        )
    }

    if (sortSheetOpen) {
        SortSheet(
            sortField = ui.sort.field,
            sortOrder = ui.sort.order,
            onFieldChange = { uiVm.setSortField(it) },
            onOrderChange = { uiVm.setSortOrder(it) },
            onDismiss = { sortSheetOpen = false }
        )
    }
}

// ---------------------- 底部弹层 / 卡片 ----------------------

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

            Text("price range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            var tmpRange by remember { mutableStateOf(priceMin..priceMax) }
            Text("$${tmpRange.start.roundToInt()} - $${tmpRange.endInclusive.roundToInt()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            RangeSlider(
                value = tmpRange,
                onValueChange = { r ->
                    val s = r.start.coerceIn(0f, 2000f)
                    val e = r.endInclusive.coerceIn(0f, 2000f)
                    tmpRange = s..e
                },
                valueRange = 0f..2000f,
                steps = 19
            )

            Spacer(Modifier.height(16.dp))
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("free shipping", style = MaterialTheme.typography.titleMedium)
                Switch(checked = onlyFreeShipping, onCheckedChange = onOnlyFreeShippingChange)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("stock availability", style = MaterialTheme.typography.titleMedium)
                Switch(checked = onlyInStock, onCheckedChange = onOnlyInStockChange)
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClear) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onPriceChange(tmpRange.start, tmpRange.endInclusive); onApply() }) { Text("Apply") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

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
            Text("Choose a field and order", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Column {
                SortField.values().forEach { f ->
                    ListItem(
                        headlineContent = { Text(f.label) },
                        trailingContent = {
                            RadioButton(selected = (sortField == f), onClick = { onFieldChange(f) })
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
                    leadingIcon = { Icon(Icons.Outlined.ArrowUpward, null) }
                )
                FilterChip(
                    selected = (sortOrder == SortOrder.Desc),
                    onClick = { onOrderChange(SortOrder.Desc) },
                    label = { Text("High to Low") },
                    leadingIcon = { Icon(Icons.Outlined.ArrowDownward, null) }
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

@Composable
private fun ProductCard(
    product: Product,
    onCompareClick: (Product) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFDDEEE0)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Image, contentDescription = null) }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium)
                StarsRow(rating = product.rating)
                Spacer(Modifier.height(6.dp))
                Text("$" + "%.2f".format(product.price), style = MaterialTheme.typography.titleSmall)

                val platformText = when (product.platform) {
                    Platform.Amazon -> "Amazon"
                    Platform.BestBuy -> "BestBuy"
                }
                Text(
                    platformText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))
                Button(onClick = { onCompareClick(product) }) { Text("COMPARE") }
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
