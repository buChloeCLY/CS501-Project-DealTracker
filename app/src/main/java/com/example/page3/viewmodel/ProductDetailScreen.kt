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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.page3.model.HistoryPointDto
import com.example.page3.model.PlatformPrice
import com.example.page3.model.PricePoint
import com.example.page3.model.Product
import com.example.page3.ui.theme.AppColors
import com.example.page3.ui.theme.AppDimens
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas


// ---------------------- TOP-LEVEL SCREEN ----------------------

@Composable
fun ProductDetailScreen(
    pid:Int,
    name: String,
    price: Double,
    rating: Float,
    source: String,
    navController: NavController
) {
    val viewModel: ProductViewModel = viewModel()


    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ✅ ViewModel states
    val platformPrices by viewModel.platformPrices.collectAsState()
    val priceHistoryState by viewModel.priceHistory.collectAsState()

    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.loadPlatformPrices(pid = 1)
        viewModel.loadPriceHistory(pid = 1, days = 7)
    }

    val product = Product(
        pid=1,
        name = name,
        color = "Black",
        storage = "128GB",
        currentPrice = price,
        originalPrice = price * 1.15,
        imageUrl = ""
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

            // ✅ 历史 Loading / Error fallback
            when {
                priceHistoryState.loading -> {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading history…")
                    }
                }
                priceHistoryState.data.isEmpty() -> {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No history available1")
                    }
                }

                else -> {
                    PriceHistoryChart(
                        data = priceHistoryState.data.takeLast(7),
                        height = 220.dp,
                        yTickCount = 5,
                        xLabelTilted = true,
                        lineStrokeWidth = 2.dp
                    )
                }
            }

            PlatformPriceCardList(
                items = platformPrices,
                onItemClick = { platformName ->
                    scope.launch { snackbarHostState.showSnackbar("Open $platformName") }
                }
            )
        }
    }
}

// ---------------------- Product Header ----------------------

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
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}

// ---------------------- Price History Chart ----------------------

@Composable
private fun PriceHistoryChart(
    data: List<HistoryPointDto>,
    height: Dp,
    yTickCount: Int,
    xLabelTilted: Boolean,
    lineStrokeWidth: Dp
) {
    if (data.isEmpty()) return

    val leftAxisSpace = 48.dp
    val bottomAxisSpace = 48.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height + bottomAxisSpace)
                .padding(start = leftAxisSpace, top = 16.dp, end = 16.dp)
        ) {
            val prices = data.map { it.price }
            val minVal = prices.min()
            val maxVal = prices.max()
            val (yMin, yMax, step) = niceAxis(minVal, maxVal, yTickCount)

            val chartHeight = height.toPx()
            val chartWidth = size.width
            val stepCount = ((yMax - yMin) / step).toInt()

            // prepare paints
            val yLabelPaint = android.graphics.Paint().apply {
                color = AppColors.PrimaryText.toArgb()
                textSize = 30f
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }

            val xLabelPaint = android.graphics.Paint().apply {
                color = AppColors.PrimaryText.toArgb()
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            // === horizontal grid lines + y labels ===
            for (i in 0..stepCount) {
                val yy = chartHeight - (i * (chartHeight / stepCount))

                // grid
                drawLine(
                    color = AppColors.ChartLine,
                    start = androidx.compose.ui.geometry.Offset(0f, yy),
                    end = androidx.compose.ui.geometry.Offset(chartWidth, yy),
                    strokeWidth = 1f
                )

                // label
                val label = (yMin + step * i).toInt().toString()
                drawIntoCanvas { cnv ->
                    cnv.nativeCanvas.drawText(
                        label,
                        -24.dp.toPx(),
                        yy + 8f,
                        yLabelPaint
                    )
                }
            }

            // === x labels ===
            val xCount = (data.size - 1).coerceAtLeast(1)
            data.forEachIndexed { i, p ->
                val xx = i * (chartWidth / xCount)
                val baseY = chartHeight + 20f

                drawIntoCanvas { cnv ->
                    val nc = cnv.nativeCanvas
                    if (xLabelTilted) {
                        nc.save()
                        nc.rotate(-45f, xx, baseY)
                    }
                    nc.drawText(
                        p.date,
                        xx,
                        baseY + 20f,
                        xLabelPaint
                    )
                    if (xLabelTilted) {
                        nc.restore()
                    }
                }
            }

            // === price line ===
            val path = Path()
            data.forEachIndexed { i, p ->
                val x = i * (chartWidth / xCount)
                val denom = (yMax - yMin).takeIf { it > 0 } ?: 1.0
                val ratio = (p.price - yMin) / denom
                val y = chartHeight - (ratio * chartHeight).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path,
                AppColors.ChartLine,
                style = Stroke(lineStrokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}


// ---------------------- Platform Price Cards ----------------------

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

// ---------------------- AXIS UTILITY ----------------------

private data class AxisInfo(val min: Double, val max: Double, val step: Double)

private fun niceAxis(minVal: Double, maxVal: Double, tickCount: Int): AxisInfo {
    val d = (maxVal - minVal)
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
