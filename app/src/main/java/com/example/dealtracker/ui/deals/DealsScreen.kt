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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.deals.viewmodel.DealsViewModel
import com.example.dealtracker.ui.deals.viewmodel.SortField
import com.example.dealtracker.ui.deals.viewmodel.SortOrder
import kotlin.math.roundToInt

@Composable
fun DealsScreen(
    showBack: Boolean,
    onBack: () -> Unit,
    category: String? = null,      // 分类点击过来的
    searchQuery: String? = null,   // 搜索框过来的
    onCompareClick: (Product) -> Unit,
    viewModel: DealsViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()

    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 有 category → 按分类过滤
    // 没 category 有 searchQuery → 走搜索 API
    // 都没有 → 加载全部
    LaunchedEffect(searchQuery, category) {
        when {
            category != null -> viewModel.applyCategory(category)
            searchQuery != null -> viewModel.applySearch(searchQuery)
            else -> viewModel.loadProducts()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            // Loading 视图
            if (ui.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading...")
                }
            }
            // 错误视图
            else if (ui.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Error,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Error: ${ui.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshProducts() }) {
                        Icon(Icons.Filled.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
            // 主内容
            else {
                Column(Modifier.fillMaxSize()) {

                    // ---------------- 搜索状态 + 分页指示 ----------------
                    if (ui.searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Searching: \"${ui.searchQuery}\" (Page ${ui.currentPage} / ${ui.totalPages})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 若当前页没有搜到内容
                    if (ui.searchQuery.isNotBlank() && ui.products.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No results for \"${ui.searchQuery}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        return@Box
                    }

                    // ---------------- Filter / Sort / Refresh ----------------
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AssistChip(
                            onClick = { filterSheetOpen = true },
                            label = { Text("Filter") },
                            leadingIcon = { Icon(Icons.Outlined.FilterList, null) }
                        )
                        AssistChip(
                            onClick = { sortSheetOpen = true },
                            label = { Text("Sort") },
                            leadingIcon = { Icon(Icons.Outlined.Sort, null) }
                        )
                        AssistChip(
                            onClick = { viewModel.refreshProducts() },
                            label = { Text("Refresh") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) }
                        )
                    }

                    // ---------------- Product 列表 ----------------
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // 数量提示
                        item {
                            Text(
                                "${ui.filteredSorted.size} products",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(ui.filteredSorted) { p ->
                            ProductCard(
                                product = p,
                                onCompareClick = onCompareClick
                            )
                        }

                        // ---------------- 分页控件 ----------------
                        if (ui.searchQuery.isNotBlank()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadPrevPage() },
                                        enabled = ui.currentPage > 1
                                    ) {
                                        Text("Prev")
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.loadNextPage() },
                                        enabled = ui.currentPage < ui.totalPages
                                    ) {
                                        Text("Next")
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------------- Back to Top ----------------
                val showScrollTop by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 4 }
                }

                AnimatedVisibility(
                    visible = showScrollTop,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } }
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Top")
                    }
                }
            }
        }
    }

    // ---------------- BottomSheet: Filter ----------------
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
                    Platform.eBay -> viewModel.toggleBestBuy(checked)
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

    // ---------------- BottomSheet: Sort ----------------
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
// ===================================
// 产品卡片（带图片）
// ===================================

@Composable
private fun ProductCard(
    product: Product,
    onCompareClick: (Product) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCompareClick(product) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🖼️ 产品图片
            ProductImage(
                imageUrl = product.imageUrl,
                title = product.title,
                modifier = Modifier.size(100.dp)
            )

            // 产品信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // 评分
                StarsRow(rating = product.rating)

                Spacer(Modifier.height(8.dp))

                // 价格
                Text(
                    text = product.priceText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // 来源信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 平台标签
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                product.platform.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )

                    // 包邮图标
                    if (product.freeShipping) {
                        Icon(
                            Icons.Outlined.LocalShipping,
                            contentDescription = "Free Shipping",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 有货图标
                    if (product.inStock) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "In Stock",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 比较按钮
                OutlinedButton(
                    onClick = { onCompareClick(product) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("COMPARE")
                }
            }
        }
    }
}

// ===================================
// 产品图片组件（支持加载、错误、占位）
// ===================================

@Composable
private fun ProductImage(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            // 使用 SubcomposeAsyncImage 支持自定义 loading 和 error 状态
            coil.compose.SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    // 加载中显示进度条
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                },
                error = {
                    // 加载失败显示占位图标
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.BrokenImage,
                            contentDescription = "Image not available",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } else {
            // 没有图片时显示占位图标
            Icon(
                Icons.Outlined.Image,
                contentDescription = "No image",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================================
// 评分星星
// ===================================

@Composable
private fun StarsRow(rating: Float, max: Int = 5) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { idx ->
            val filled = idx < rating.toInt()
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (filled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ===================================
// Filter Sheet（保持不变）
// ===================================

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
                onValueChange = { tmpRange = it.start.coerceIn(0f, 2000f)..it.endInclusive.coerceIn(0f, 2000f) },
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
                    onClick = { onPlatformToggle(Platform.eBay, !chooseBestBuy) },
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

// ===================================
// Sort Sheet（保持不变）
// ===================================

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
            Spacer(Modifier.height(16.dp))

            Column {
                SortField.values().forEach { f ->
                    ListItem(
                        headlineContent = { Text(f.label) },
                        trailingContent = {
                            RadioButton(
                                selected = (sortField == f),
                                onClick = { onFieldChange(f) }
                            )
                        }
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