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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.request.ImageRequest
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.deals.viewmodel.DealsViewModel
import com.example.dealtracker.ui.deals.viewmodel.SortField
import com.example.dealtracker.ui.deals.viewmodel.SortOrder
import com.example.dealtracker.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun DealsScreen(
    showBack: Boolean,
    onBack: () -> Unit,
    category: String? = null,
    searchQuery: String? = null,
    onCompareClick: (Product) -> Unit,
    viewModel: DealsViewModel = viewModel()
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale
    val ui by viewModel.uiState.collectAsState()

    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(searchQuery, category) {
        when {
            !category.isNullOrBlank() -> viewModel.applyCategory(category)
            !searchQuery.isNullOrBlank() -> viewModel.applySearch(searchQuery)
            else -> viewModel.loadProducts()
        }
    }


    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Deals",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = (22 * fontScale).sp
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground,
                    titleContentColor = colors.topBarContent,
                    navigationIconContentColor = colors.topBarContent
                )
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
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
            else {
                Column(Modifier.fillMaxSize()) {

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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

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
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        containerColor = colors.accent
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Top")
                    }
                }
            }
        }
    }

    if (filterSheetOpen) {
        FilterSheet(
            priceMin = ui.filters.priceMin,
            priceMax = ui.filters.priceMax,
            onPriceChange = { min, max -> viewModel.setPrice(min, max) },
            chooseAmazon = ui.filters.chooseAmazon,
            chooseEBay = ui.filters.chooseEBay,
            chooseWalmart = ui.filters.chooseWalmart,
            onPlatformToggle = { p, checked ->
                when (p) {
                    Platform.Amazon -> viewModel.toggleAmazon(checked)
                    Platform.eBay -> viewModel.toggleEBay(checked)
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

// Product card with image
@Composable
private fun ProductCard(
    product: Product,
    onCompareClick: (Product) -> Unit
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

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
            ProductImage(
                imageUrl = product.imageUrl,
                title = product.title,
                modifier = Modifier.size(100.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    fontSize = (16 * fontScale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                StarsRow(rating = product.rating)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = product.priceText,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = (20 * fontScale).sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    product.platformList.forEach { platformName ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = platformName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }

                    if (product.freeShipping) {
                        Icon(
                            Icons.Outlined.LocalShipping,
                            contentDescription = "Free Shipping",
                            modifier = Modifier.size(16.dp),
                            tint = colors.accent
                        )
                    }

                    if (product.inStock) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "In Stock",
                            modifier = Modifier.size(16.dp),
                            tint = colors.success
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

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

// Product image component with loading and error states
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
            coil.compose.SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Inside,
                modifier = Modifier.fillMaxSize(),
                loading = {
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
            Icon(
                Icons.Outlined.Image,
                contentDescription = "No image",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Star rating display
@Composable
private fun StarsRow(rating: Float, max: Int = 5) {
    val colors = AppTheme.colors

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { idx ->
            val filled = idx < rating.toInt()
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (filled) colors.ratingColor else MaterialTheme.colorScheme.outline,
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

// Filter bottom sheet
@Composable
private fun FilterSheet(
    priceMin: Float,
    priceMax: Float,
    onPriceChange: (Float, Float) -> Unit,
    chooseAmazon: Boolean,
    chooseEBay: Boolean,
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
    val fontScale = AppTheme.fontScale

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Filter",
                style = MaterialTheme.typography.titleLarge,
                fontSize = (22 * fontScale).sp
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "Price Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = (16 * fontScale).sp
            )
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

            Text(
                "Platform",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = (16 * fontScale).sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chooseAmazon,
                    onClick = { onPlatformToggle(Platform.Amazon, !chooseAmazon) },
                    label = { Text("Amazon") }
                )
                FilterChip(
                    selected = chooseEBay,
                    onClick = { onPlatformToggle(Platform.eBay, !chooseEBay) },
                    label = { Text("eBay") }
                )
                FilterChip(
                    selected = chooseWalmart,
                    onClick = { onPlatformToggle(Platform.Walmart, !chooseWalmart) },
                    label = { Text("Walmart") }
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Free Shipping",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = (16 * fontScale).sp
                )
                Switch(checked = onlyFreeShipping, onCheckedChange = onOnlyFreeShippingChange)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "In Stock",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = (16 * fontScale).sp
                )
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

// Sort bottom sheet
@Composable
private fun SortSheet(
    sortField: SortField,
    sortOrder: SortOrder,
    onFieldChange: (SortField) -> Unit,
    onOrderChange: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val fontScale = AppTheme.fontScale

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Sort",
                style = MaterialTheme.typography.titleLarge,
                fontSize = (22 * fontScale).sp
            )
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
            Text(
                "Order",
                style = MaterialTheme.typography.titleMedium,
                fontSize = (16 * fontScale).sp
            )
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