package com.example.dealtracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dealtracker.domain.model.PricePoint
import kotlin.math.*

/**
 * 可滚动的价格历史图表
 * 支持30天数据，7天窗口，左右拖动查看，15%波动标注
 */
@Composable
fun ScrollablePriceChart(
    priceHistory: List<PricePoint>,
    modifier: Modifier = Modifier
) {
    // 滑动窗口：7天
    var scrollOffset by remember { mutableStateOf(0f) }
    var selectedPoint by remember { mutableStateOf<PricePoint?>(null) }
    val windowSize = 7
    val totalDataPoints = priceHistory.size
    val maxScroll = (totalDataPoints - windowSize).coerceAtLeast(0)

    // 默认显示最新数据（最右边）
    LaunchedEffect(totalDataPoints) {
        scrollOffset = maxScroll.toFloat()
    }
    val startIndex = scrollOffset.toInt().coerceIn(0, maxScroll)
    val endIndex = (startIndex + windowSize).coerceAtMost(totalDataPoints)
    val visibleData = if (priceHistory.isNotEmpty()) {
        priceHistory.subList(startIndex, endIndex)
    } else {
        emptyList()
    }

    val volatilePoints = remember(priceHistory) {
        detectVolatilePoints(priceHistory, threshold = 0.15)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Price History (${totalDataPoints} Days)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (totalDataPoints > windowSize) {
                    Text(
                        "Showing ${startIndex + 1}-${endIndex} of $totalDataPoints days",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (visibleData.isNotEmpty()) {
                Box(modifier = Modifier.height(200.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    scrollOffset = (scrollOffset - dragAmount.x / 20f)
                                        .coerceIn(0f, maxScroll.toFloat())
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    selectedPoint = findNearestPoint(
                                        tapOffset,
                                        visibleData,
                                        size.width.toFloat(),
                                        size.height.toFloat()
                                    )
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val padding = 40f
                        val bottomPadding = 35f
                        val chartWidth = width - padding * 2
                        val chartHeight = height - padding - bottomPadding

                        if (visibleData.isEmpty()) return@Canvas

                        val minPrice = visibleData.minOf { it.price }
                        val maxPrice = visibleData.maxOf { it.price }
                        val priceRange = maxPrice - minPrice
                        val axis = niceAxis(minPrice, maxPrice, 5)

                        drawGridLines(axis, padding, chartWidth, chartHeight)
                        drawYAxisLabels(axis, padding, chartHeight)

                        val path = Path()
                        val points = mutableListOf<Offset>()

                        visibleData.forEachIndexed { index, point ->
                            val x = padding + (index.toFloat() / (visibleData.size - 1).coerceAtLeast(1)) * chartWidth
                            val normalizedPrice = if (priceRange > 0) {
                                (point.price - axis.min) / (axis.max - axis.min)
                            } else 0.5
                            val y = padding + chartHeight - (normalizedPrice * chartHeight).toFloat()

                            points.add(Offset(x, y))

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF4CAF50),
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )

                        points.forEachIndexed { index, point ->
                            val actualPoint = visibleData[index]
                            val isVolatile = volatilePoints.contains(startIndex + index)
                            val isSelected = selectedPoint == actualPoint

                            drawCircle(
                                color = if (isSelected) Color(0xFF2196F3) else Color(0xFF4CAF50),
                                radius = if (isSelected) 8f else 6f,
                                center = point
                            )

                            if (isVolatile) {
                                drawVolatileMarker(point, visibleData, index)
                            }
                        }

                        val labelStep = if (visibleData.size > 5) 2 else 1
                        visibleData.forEachIndexed { index, point ->
                            if (index % labelStep == 0 || index == visibleData.size - 1) {
                                val x = padding + (index.toFloat() / (visibleData.size - 1).coerceAtLeast(1)) * chartWidth
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 24f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    canvas.nativeCanvas.drawText(
                                        formatDate(point.date),
                                        x,
                                        height - 15f,
                                        paint
                                    )
                                }
                            }
                        }
                    }

                    selectedPoint?.let { point ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2196F3),
                                shadowElevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        point.date,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "$${"%.2f".format(point.price)}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (totalDataPoints > windowSize) {
                    ScrollIndicator(
                        currentPosition = scrollOffset,
                        maxPosition = maxScroll.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No price history available", color = Color.Gray)
                }
            }
        }
    }
}

/**
 * 检测价格波动剧烈的点
 * threshold: 波动阈值，0.15表示15%
 */
private fun detectVolatilePoints(
    data: List<PricePoint>,
    threshold: Double
): Set<Int> {
    val volatileIndices = mutableSetOf<Int>()

    for (i in 1 until data.size) {
        val prevPrice = data[i - 1].price
        val currPrice = data[i].price
        val changePercent = abs((currPrice - prevPrice) / prevPrice)

        if (changePercent >= threshold) {
            volatileIndices.add(i)
        }
    }

    return volatileIndices
}

/**
 * 绘制波动标记
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVolatileMarker(
    point: Offset,
    data: List<PricePoint>,
    index: Int
) {
    if (index == 0) return

    val prevPrice = data[index - 1].price
    val currPrice = data[index].price
    val isIncrease = currPrice > prevPrice

    val arrowPath = Path().apply {
        if (isIncrease) {
            moveTo(point.x, point.y - 30f)
            lineTo(point.x - 5f, point.y - 22f)
            lineTo(point.x + 5f, point.y - 22f)
            close()
        } else {
            moveTo(point.x, point.y + 30f)
            lineTo(point.x - 5f, point.y + 22f)
            lineTo(point.x + 5f, point.y + 22f)
            close()
        }
    }

    drawPath(
        path = arrowPath,
        color = Color(0xFFE53935)
    )

    val changePercent = abs((currPrice - prevPrice) / prevPrice * 100)
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            textSize = 20f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.nativeCanvas.drawText(
            "${if (isIncrease) "+" else "-"}${"%.1f".format(changePercent)}%",
            point.x,
            if (isIncrease) point.y - 35f else point.y - 15f,
            paint
        )
    }
}

/**
 * 绘制网格线
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLines(
    axis: AxisInfo,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    var value = axis.min
    while (value <= axis.max + 0.001) {
        val normalizedY = (value - axis.min) / (axis.max - axis.min)
        val y = padding + chartHeight - (normalizedY * chartHeight).toFloat()

        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1f
        )

        value += axis.step
    }
}

/**
 * 绘制Y轴标签
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawYAxisLabels(
    axis: AxisInfo,
    padding: Float,
    chartHeight: Float
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        var value = axis.min
        while (value <= axis.max + 0.001) {
            val normalizedY = (value - axis.min) / (axis.max - axis.min)
            val y = padding + chartHeight - (normalizedY * chartHeight).toFloat()

            canvas.nativeCanvas.drawText(
                "$${"%.0f".format(value)}",
                padding - 10f,
                y + 8f,
                paint
            )

            value += axis.step
        }
    }
}

/**
 * 找到最近的数据点
 */
private fun findNearestPoint(
    tapOffset: Offset,
    data: List<PricePoint>,
    width: Float,
    height: Float
): PricePoint? {
    val padding = 40f
    val chartWidth = width - padding * 2
    val chartHeight = height - padding * 2

    if (data.isEmpty()) return null

    val minPrice = data.minOf { it.price }
    val maxPrice = data.maxOf { it.price }
    val axis = niceAxis(minPrice, maxPrice, 5)
    val priceRange = axis.max - axis.min

    var closestPoint: PricePoint? = null
    var minDistance = Float.MAX_VALUE

    data.forEachIndexed { index, point ->
        val x = padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
        val normalizedPrice = if (priceRange > 0) {
            (point.price - axis.min) / priceRange
        } else 0.5
        val y = padding + chartHeight - (normalizedPrice * chartHeight).toFloat()

        val distance = sqrt(
            (tapOffset.x - x).pow(2) + (tapOffset.y - y).pow(2)
        )

        if (distance < minDistance && distance < 50f) { // 50px触摸半径
            minDistance = distance
            closestPoint = point
        }
    }

    return closestPoint
}

/**
 * 滚动指示器
 */
@Composable
private fun ScrollIndicator(
    currentPosition: Float,
    maxPosition: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
    ) {
        val progress = if (maxPosition > 0) currentPosition / maxPosition else 0f
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
        )
    }
}

/**
 * 格式化日期显示
 */
private fun formatDate(date: String): String {
    return try {
        val parts = date.split("-")
        if (parts.size == 3) {
            "${parts[1].toInt()}/${parts[2].toInt()}"
        } else {
            date.takeLast(5)
        }
    } catch (e: Exception) {
        date.takeLast(5)
    }
}

private data class AxisInfo(val min: Double, val max: Double, val step: Double)

/**
 * 计算漂亮的坐标轴
 */
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