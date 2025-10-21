package com.example.dealtrackerv1.ui.product

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dealtrackerv1.model.PlatformPrice
import com.example.dealtrackerv1.model.PricePoint
import com.example.dealtrackerv1.model.Product
import com.example.dealtrackerv1.ui.theme.AppColors
import com.example.dealtrackerv1.ui.theme.AppDimens
import com.example.dealtrackerv1.viewmodel.ProductViewModel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel = remember { ProductViewModel() }

    val product = viewModel.getProduct()
    val priceHistory = viewModel.getPriceHistory()
    val platformPrices = viewModel.getPlatformPrices()

    Scaffold(
        topBar = { ProductDetailTopBar(product.name) },
        bottomBar = { ProductDetailBottomBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(innerPadding)
                .padding(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            ProductHeader(product = product)
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()   // ✅ 新增
            val viewModel = remember { ProductViewModel() }

            PriceHistoryChart(
                data = priceHistory,
                height = 220.dp,
                yTickCount = 5,
                xLabelTilted = true, // S（倾斜）
                lineStrokeWidth = 2.dp // 细线
            )

            PlatformPriceCardList(
                items = platformPrices,
                onItemClick = { name ->
                    scope.launch {                             // ✅ coroutine scope
                        snackbarHostState.showSnackbar("Open $name (TODO)")
                    }
                }
            )

        }
    }
}

/* -------------------------- Top & Bottom Bar --------------------------- */

@Composable
fun ProductDetailTopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { /* TODO: Navigation back */ }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(onClick = { /* TODO: Search */ }) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
    }
}

@Composable
fun ProductDetailBottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Card)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Home, contentDescription = "Home")
        Icon(Icons.Filled.ShoppingCart, contentDescription = "Deals")
        Icon(Icons.Filled.List, contentDescription = "Lists")
        Icon(Icons.Filled.Person, contentDescription = "Profile")
    }
}

/* ----------------------------- Header --------------------------------- */

@Composable
private fun ProductHeader(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // 大圆角 + 明显阴影（B）
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.CardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 占位图（可后续替换为 Coil）
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(AppColors.SecondaryText.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Image", color = AppColors.SecondaryText)
                }

                IconButton(onClick = { /* TODO: Add to list */ }) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = AppDimens.TitleText)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name,
                color = AppColors.PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TitleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${product.color}, ${product.storage}",
                color = AppColors.SecondaryText,
                fontSize = AppDimens.BodyText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$${product.currentPrice}",
                    color = AppColors.Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = (AppDimens.TitleText * 1.1f)
                )

                Text(
                    text = "$${product.originalPrice}",
                    color = AppColors.SecondaryText,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}

/* --------------------------- Price Chart ------------------------------- */

@Composable
private fun PriceHistoryChart(
    data: List<PricePoint>,
    height: Dp,
    yTickCount: Int,
    xLabelTilted: Boolean,
    lineStrokeWidth: Dp
) {
    if (data.isEmpty()) return

    // 轴与图形区域的留白（左侧为 Y 轴标签留白、底部为 X 轴标签留白）
    val leftAxisSpace = 48.dp
    val bottomAxisSpace = 36.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height + bottomAxisSpace) // 额外空间给 X 轴标签
                .padding(AppDimens.CardPadding)
        ) {
            // 计算 Y 轴范围与刻度
            val prices = data.map { it.price }
            val minPriceRaw = prices.min()
            val maxPriceRaw = prices.max()
            val (yMin, yMax, yStep) = niceAxis(minPriceRaw, maxPriceRaw, yTickCount)

            // 画布区域（扣除坐标轴留白）
            val chartTopPadding = 8.dp
            val chartRightPadding = 12.dp

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .align(Alignment.TopStart)
                    .padding(
                        start = leftAxisSpace,
                        end = chartRightPadding,
                        top = chartTopPadding,
                        bottom = 0.dp
                    )
            ) {
                val w = size.width
                val h = size.height

                // 画 X/Y 轴
                drawLine(
                    color = AppColors.SecondaryText.copy(alpha = 0.6f),
                    start = androidx.compose.ui.geometry.Offset(0f, h),
                    end = androidx.compose.ui.geometry.Offset(w, h),
                    strokeWidth = 1f
                )
                drawLine(
                    color = AppColors.SecondaryText.copy(alpha = 0.6f),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, h),
                    strokeWidth = 1f
                )

                // Y 轴网格线
                var yVal = yMin
                while (yVal <= yMax + 1e-6) {
                    val ratio = (yVal - yMin) / (yMax - yMin).coerceAtLeast(1.0)
                    val y = h - (ratio * h).toFloat()
                    drawLine(
                        color = AppColors.SecondaryText.copy(alpha = 0.2f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 1f
                    )
                    yVal += yStep
                }

                // 折线（细线）
                val path = Path()
                val n = data.size
                val xStep = if (n > 1) w / (n - 1) else 0f
                data.forEachIndexed { index, point ->
                    val xr = if (n == 1) 0f else index / (n - 1f)
                    val x = xr * w
                    val yRatio = (point.price - yMin) / (yMax - yMin).coerceAtLeast(1.0)
                    val y = h - (yRatio * h).toFloat()

                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = AppColors.ChartLine,
                    style = Stroke(width = lineStrokeWidth.toPx(), cap = StrokeCap.Round)
                )

                // 小圆点
                data.forEachIndexed { index, point ->
                    val x = if (n == 1) 0f else index * xStep
                    val yRatio = (point.price - yMin) / (yMax - yMin).coerceAtLeast(1.0)
                    val y = h - (yRatio * h).toFloat()
                    drawCircle(
                        color = AppColors.ChartLine,
                        radius = 3.5f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }

            // 左侧 Y 轴标签（与网格一致）
            Column(
                modifier = Modifier
                    .width(leftAxisSpace)
                    .height(height)
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 从顶部到底部显示 yMax -> yMin（更直观）
                val labels = mutableListOf<Int>()
                run {
                    var y = yMin
                    while (y <= yMax + 1e-6) {
                        labels.add(y.toInt())
                        y += yStep
                    }
                }
                val topToBottom = labels.reversed()
                topToBottom.forEach { v ->
                    Text(
                        text = "$$v",
                        color = AppColors.SecondaryText,
                        fontSize = AppDimens.BodyText
                    )
                }
            }

            // 底部 X 轴标签（倾斜 S）
            if (data.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(start = leftAxisSpace),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    data.forEach { p ->
                        Text(
                            text = p.date,
                            color = AppColors.SecondaryText,
                            fontSize = AppDimens.BodyText,
                            modifier = if (xLabelTilted)
                                Modifier.graphicsLayer { rotationZ = -30f }
                            else Modifier
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------- Platform Price Card List ----------------------- */

@Composable
private fun PlatformPriceCardList(
    items: List<PlatformPrice>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = true
    ) {
        items(items) { row ->
            PlatformPriceCard(row, onItemClick)
        }
    }
}

@Composable
private fun PlatformPriceCard(
    row: PlatformPrice,
    onItemClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp) // ② 选择更高卡片
            .clickable { onItemClick(row.platformName) },
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // B 明显阴影
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 占位图标（你之后可换成品牌 icon 或图片）
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AppColors.SecondaryText.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null
                    )
                }
                Column {
                    Text(
                        text = row.platformName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = AppColors.PrimaryText
                        )
                    )
                    Text(
                        text = "Fast delivery • Official store",
                        color = AppColors.SecondaryText,
                        fontSize = AppDimens.BodyText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$${row.price}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PrimaryText
                    )
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = AppColors.SecondaryText
                )
            }
        }
    }
}

/* ------------------------------ Utils ---------------------------------- */

private data class AxisInfo(val min: Double, val max: Double, val step: Double)

/**
 * 计算“好看”的坐标轴范围与刻度间距（nice numbers）
 * 输入为数据 min/max 与希望的刻度数，输出为对齐后的 min/max/step
 */
private fun niceAxis(minVal: Int, maxVal: Int, tickCount: Int): AxisInfo {
    if (minVal == maxVal) {
        // 单点情况，给一个上下各 1 的范围
        return AxisInfo(minVal - 1.0, maxVal + 1.0, 1.0)
    }
    val d = (maxVal - minVal).toDouble()
    val rawStep = d / (tickCount - 1).coerceAtLeast(1)
    val niceStep = niceNumber(rawStep, round = true)
    val niceMin = floor(minVal / niceStep) * niceStep
    val niceMax = ceil(maxVal / niceStep) * niceStep
    return AxisInfo(niceMin, niceMax, niceStep)
}

/**
 * Nice number 算法（Graphics Gems）
 * 将任意正数调整到 1、2、5 系列乘以 10^n 的形式，便于坐标轴显示
 */
private fun niceNumber(x: Double, round: Boolean): Double {
    val exp = kotlin.math.floor(kotlin.math.log10(x))
    val f = x / 10.0.pow(exp)
    val nf = when {
        round && f < 1.5 -> 1.0
        round && f < 3.0 -> 2.0
        round && f < 7.0 -> 5.0
        round -> 10.0
        !round && f <= 1.0 -> 1.0
        !round && f <= 2.0 -> 2.0
        !round && f <= 5.0 -> 5.0
        else -> 10.0
    }
    return nf * 10.0.pow(exp)
}
