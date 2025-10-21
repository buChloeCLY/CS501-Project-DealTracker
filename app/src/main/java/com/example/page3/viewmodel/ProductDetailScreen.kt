@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.page3.viewmodel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.page3.model.PlatformPrice
import com.example.page3.model.PricePoint
import com.example.page3.model.Product
import com.example.page3.ui.theme.AppColors
import com.example.page3.ui.theme.AppDimens
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

@Composable
fun ProductDetailScreen(
    name: String,
    price: Double,
    rating: Float,
    source: String,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 用从 NavGraph 传进来的参数构造 Product
    val product = Product(
        name = name,
        color = "Black",
        storage = "128GB",
        currentPrice = price,
        originalPrice = price * 1.15,
        imageUrl = ""
    )

    // 临时假数据（后面可替换）
    val priceHistory = listOf(
        PricePoint("09/01", price + 50),
        PricePoint("09/10", price + 20),
        PricePoint("09/20", price),
        PricePoint("09/25", price - 40),
    )
    val platformPrices = listOf(
        PlatformPrice("Amazon", "", price),
        PlatformPrice("BestBuy", "",price + 20),
        PlatformPrice("Apple Store", "",price + 50),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductHeader(product)

            PriceHistoryChart(
                data = priceHistory,
                height = 220.dp,
                yTickCount = 5,
                xLabelTilted = true,
                lineStrokeWidth = 2.dp
            )

            PlatformPriceCardList(
                items = platformPrices,
                onItemClick = { platformName ->
                    scope.launch { snackbarHostState.showSnackbar("Open $platformName") }
                }
            )
        }
    }
}

@Composable
private fun ProductHeader(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card)
    ) {
        Column(Modifier.padding(AppDimens.CardPadding)) {
            Text(product.name, fontSize = AppDimens.TitleText, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText)
            Text("${product.color}, ${product.storage}", color = AppColors.SecondaryText)

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$${product.currentPrice}",
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TitleText,
                    color = AppColors.Accent
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$${product.originalPrice}",
                    color = AppColors.SecondaryText,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
            }
        }
    }
}

@Composable
private fun PriceHistoryChart(
    data: List<PricePoint>,
    height: Dp,
    yTickCount: Int,
    xLabelTilted: Boolean,
    lineStrokeWidth: Dp
) {
    if (data.isEmpty()) return

    val leftAxisSpace = 48.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card)
    ) {
        Canvas(
            modifier = Modifier
                .height(height)
                .padding(start = leftAxisSpace, top = 16.dp, end = 16.dp)
        ) {
            val prices = data.map { it.price }
            val min = prices.min()
            val max = prices.max()
            val (yMin, yMax, yStep) = niceAxis(min, max, yTickCount)

            val w = size.width
            val h = size.height
            val path = Path()

            data.forEachIndexed { i, p ->
                val x = i * (w / (data.size - 1))
                val ratio = (p.price - yMin) / (yMax - yMin).coerceAtLeast(1.0)
                val y = h - (ratio * h).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, AppColors.ChartLine, style = Stroke(lineStrokeWidth.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun PlatformPriceCardList(
    items: List<PlatformPrice>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { row ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable { onItemClick(row.platformName) },
                shape = RoundedCornerShape(AppDimens.CornerRadius),
                colors = CardDefaults.cardColors(containerColor = AppColors.Card)
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.platformName, fontWeight = FontWeight.Medium, color = AppColors.PrimaryText)
                    Text("$${row.price}", fontWeight = FontWeight.Bold, color = AppColors.Accent)
                }
            }
        }
    }
}

private data class AxisInfo(val min: Double, val max: Double, val step: Double)

private fun niceAxis(minVal: Double, maxVal: Double, tickCount: Int): AxisInfo {
    val d = (maxVal - minVal).toDouble()
    val rawStep = d / (tickCount - 1).coerceAtLeast(1)
    val exp = floor(kotlin.math.log10(rawStep))
    val base = rawStep / 10.0.pow(exp)
    val step = when {
        base < 1.5 -> 1.0
        base < 3 -> 2.0
        base < 7 -> 5.0
        else -> 10.0
    } * 10.0.pow(exp)
    val niceMin = floor(minVal / step) * step
    val niceMax = ceil(maxVal / step) * step
    return AxisInfo(niceMin, niceMax, step)
}
