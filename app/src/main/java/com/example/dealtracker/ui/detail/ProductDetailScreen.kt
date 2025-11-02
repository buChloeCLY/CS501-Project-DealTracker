package com.example.dealtracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint
import com.example.dealtracker.ui.detail.viewmodel.ProductViewModel
import com.example.dealtracker.ui.theme.AppColors
import com.example.dealtracker.ui.theme.AppDimens
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    pid: Int,
    name: String,
    price: Double,
    rating: Float,
    navController: NavController
) {
    val viewModel: ProductViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ✅ 修复 1 & 2: 添加正确的 collectAsState 调用
    val platformPricesState = viewModel.platformPrices.collectAsState()
    val priceHistoryState = viewModel.priceHistory.collectAsState()

    LaunchedEffect(pid) {
        viewModel.loadPlatformPrices(pid = pid)
        viewModel.loadPriceHistory(pid = pid, days = 7)
    }

    // ✅ 修复 3 & 4: 使用 .value 获取实际值
    val platformPrices = platformPricesState.value
    val priceHistory = priceHistoryState.value

    // ✅ 修复 5 & 6: 正确使用 lambda 参数
    val currentPrice = platformPrices.minOfOrNull { platformPrice -> platformPrice.price } ?: price
    val lowestPlatform = platformPrices.minByOrNull { platformPrice -> platformPrice.price }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add tracking */ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ✅ 产品头部：显示最低价和来源
            ProductHeader(
                name = name,
                currentPrice = currentPrice,
                // ✅ 修复 7: 正确访问 platformName
                lowestPlatform = lowestPlatform?.platformName,
                hasData = platformPrices.isNotEmpty()
            )

            // ✅ 历史价格图表
            when {
                priceHistory.loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading history…", color = AppColors.SecondaryText)
                    }
                }
                priceHistory.data.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.CornerRadius),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No price history available",
                                color = AppColors.SecondaryText
                            )
                        }
                    }
                }
                else -> {
                    PriceHistoryChart(
                        data = priceHistory.data.takeLast(7),
                        height = 220.dp,
                        yTickCount = 5,
                        xLabelTilted = true,
                        lineStrokeWidth = 2.dp
                    )
                }
            }

            // ✅ 平台价格列表
            if (platformPrices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.CornerRadius),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No platform prices available",
                            color = AppColors.SecondaryText
                        )
                    }
                }
            } else {
                PlatformPriceCardList(
                    items = platformPrices,
                    onItemClick = { platformName ->
                        scope.launch { snackbarHostState.showSnackbar("Open $platformName") }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductHeader(
    name: String,
    currentPrice: Double,
    lowestPlatform: String?,
    hasData: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card)
    ) {
        Column(modifier = Modifier.padding(AppDimens.CardPadding)) {
            Text(
                name,
                fontSize = AppDimens.TitleText,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ 显示当前最低价
            if (hasData) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$${"%.2f".format(currentPrice)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TitleText,
                        color = AppColors.Accent
                    )
                    if (lowestPlatform != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "from $lowestPlatform",
                            color = AppColors.SecondaryText,
                            fontSize = AppDimens.BodyText
                        )
                    }
                }
            } else {
                Text(
                    "Price: Unknown",
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TitleText,
                    color = AppColors.SecondaryText
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
    val bottomAxisSpace = 48.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        row.platformName,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.PrimaryText
                    )
                    Text(
                        "$${"%.2f".format(row.price)}",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Accent
                    )
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
    val exp = floor(log10(rawStep))
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