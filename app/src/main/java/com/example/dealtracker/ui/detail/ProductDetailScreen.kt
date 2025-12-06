package com.example.dealtracker.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.domain.model.PricePoint
import com.example.dealtracker.ui.detail.viewmodel.ProductViewModel
import com.example.dealtracker.ui.theme.AppColors
import com.example.dealtracker.ui.theme.AppDimens
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.wishlist.WishListHolder
import com.example.dealtracker.ui.wishlist.viewmodel.WishListViewModel
import com.example.dealtracker.data.remote.repository.HistoryRepository
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    pid: Int,
    name: String,
    price: Double,
    rating: Float,
    navController: NavController,
    uid: Int = 1
) {
    val viewModel: ProductViewModel = viewModel()
    val wishlistViewModel: WishListViewModel = viewModel()
    val historyRepository = remember { HistoryRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val platformPricesState = viewModel.platformPrices.collectAsState()
    val priceHistoryState = viewModel.priceHistory.collectAsState()

    // 获取当前登录用户
    val currentUser by com.example.dealtracker.domain.UserManager.currentUser.collectAsState()
    val actualUid = currentUser?.uid ?: uid

    // 监听本地心愿单状态（即时更新）
    val wishList by WishListHolder.wishList.collectAsState()
    val isInWishlist = remember(wishList, pid) {
        wishList.any { it.pid == pid }
    }

    // 记录浏览历史（仅在用户已登录时）
    LaunchedEffect(pid, currentUser) {
        currentUser?.let { user ->
            // 用户已登录，记录浏览历史
            scope.launch {
                try {
                    historyRepository.addHistory(user.uid, pid)
                } catch (e: Exception) {
                    // 静默失败，不影响用户体验
                    android.util.Log.e("ProductDetail", "Failed to record history", e)
                }
            }
        }
    }

    LaunchedEffect(pid) {
        viewModel.loadPlatformPrices(pid = pid)
        viewModel.loadPriceHistory(pid = pid, days = 7)

    }
    val platformPrices = platformPricesState.value
    val priceHistory = priceHistoryState.value
    LaunchedEffect(platformPrices) {
        platformPrices.forEach { platform ->
            android.util.Log.d("ProductDetail", "Platform: ${platform.platformName}")
            android.util.Log.d("ProductDetail", "  price: ${platform.price}")
            android.util.Log.d("ProductDetail", "  link: '${platform.link}'")
            android.util.Log.d("ProductDetail", "  link is null: ${platform.link == null}")
        }
    }

    val currentPrice = platformPrices.minOfOrNull { it.price } ?: price
    val lowestPlatform = platformPrices.minByOrNull { it.price }

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
                    // 分享按钮
                    IconButton(onClick = {
                        shareProduct(context, pid, name, currentPrice, platformPrices)
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = AppColors.PrimaryText
                        )
                    }

                    // 收藏按钮
                    IconButton(onClick = {
                        // 检查是否登录
                        if (currentUser == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please login first")
                            }
                            navController.navigate("login")
                            return@IconButton
                        }

                        val product = Product(
                            pid = pid,
                            title = name,
                            price = price,
                            rating = rating,
                            platform = Platform.Amazon,
                            freeShipping = true,
                            inStock = true,
                            imageUrl = "",
                            category = Category.Electronics
                        )

                        if (isInWishlist) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Already in Wish List")
                            }
                            navController.navigate("wishlist")
                        } else {
                            // 使用 WishListViewModel.addProduct，同步到本地 + 后端
                            wishlistViewModel.addProduct(
                                uid = actualUid,
                                product = product,
                                alertEnabled = true
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("Added to Wish List")
                            }
                        }
                    }) {
                        Icon(
                            if (isInWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Add to WishList",
                            tint = if (isInWishlist) Color.Red else AppColors.PrimaryText
                        )
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
            // 产品头部
            ProductHeader(
                name = name,
                currentPrice = currentPrice,
                lowestPlatform = lowestPlatform?.platformName,
                hasData = platformPrices.isNotEmpty()
            )

            // 历史价格图表
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
                            Text("No price history available", color = AppColors.SecondaryText)
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

            // 平台价格列表
            Text(
                "Platform Prices",
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TitleText,
                color = AppColors.PrimaryText
            )

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
                        Text("No platform prices available", color = AppColors.SecondaryText)
                    }
                }
            } else {
                PlatformPriceCardList(
                    items = platformPrices,
                    context = context,
                    onItemClick = { url ->
                        android.util.Log.d("ProductDetail", "Clicked link: '$url'")
                        android.util.Log.d("ProductDetail", "Link isEmpty: ${url.isEmpty()}")
                        android.util.Log.d("ProductDetail", "Link isBlank: ${url.isBlank()}")

                        if (url.isNotEmpty()) {
                            android.util.Log.d("ProductDetail", "Opening URL: $url")
                            openUrl(context, url)
                        } else {
                            android.util.Log.d("ProductDetail", "Link is empty, showing snackbar")
                            scope.launch {
                                snackbarHostState.showSnackbar("Link not available")
                            }
                        }
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

    // 用于显示选中的数据点
    var selectedPoint by remember { mutableStateOf<PricePoint?>(null) }
    var tapOffset by remember { mutableStateOf<Offset?>(null) }

    val density = androidx.compose.ui.platform.LocalDensity.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card)
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height + bottomAxisSpace)
                    .padding(start = leftAxisSpace, top = 16.dp, end = 16.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            // 计算点击位置对应的数据点
                            val chartWidth = size.width.toFloat()
                            val chartHeight = height.toPx()
                            val xCount = (data.size - 1).coerceAtLeast(1)

                            // 找到最近的数据点
                            var minDist = Float.MAX_VALUE
                            var nearestPoint: PricePoint? = null
                            var nearestOffset: Offset? = null

                            val prices = data.map { it.price }
                            val minVal = prices.min()
                            val maxVal = prices.max()
                            val (yMin, yMax, _) = niceAxis(minVal, maxVal, yTickCount)

                            data.forEachIndexed { i, p ->
                                val x = i * (chartWidth / xCount)
                                val denom = (yMax - yMin).takeIf { it > 0 } ?: 1.0
                                val ratio = (p.price - yMin) / denom
                                val y = chartHeight - (ratio * chartHeight).toFloat()

                                val dist = sqrt(
                                    (offset.x - x) * (offset.x - x) +
                                            (offset.y - y) * (offset.y - y)
                                )

                                if (dist < minDist && dist < 50f) { // 50px touch radius
                                    minDist = dist
                                    nearestPoint = p
                                    nearestOffset = Offset(x, y)
                                }
                            }

                            selectedPoint = nearestPoint
                            tapOffset = nearestOffset
                        }
                    }
            ) {
                val prices = data.map { it.price }
                val minVal = prices.min()
                val maxVal = prices.max()
                val (yMin, yMax, step) = niceAxis(minVal, maxVal, yTickCount)

                val chartHeight = height.toPx()
                val chartWidth = size.width
                val stepCount = ((yMax - yMin) / step).toInt()

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

                // Y轴网格线和标签
                for (i in 0..stepCount) {
                    val yy = chartHeight - (i * (chartHeight / stepCount))
                    drawLine(
                        color = AppColors.ChartLine,
                        start = Offset(0f, yy),
                        end = Offset(chartWidth, yy),
                        strokeWidth = 1f
                    )
                    val label = (yMin + step * i).toInt().toString()
                    drawIntoCanvas { cnv ->
                        cnv.nativeCanvas.drawText(label, -24.dp.toPx(), yy + 8f, yLabelPaint)
                    }
                }

                // X轴标签
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
                        nc.drawText(p.date, xx, baseY + 20f, xLabelPaint)
                        if (xLabelTilted) {
                            nc.restore()
                        }
                    }
                }

                // 绘制折线
                val path = Path()
                data.forEachIndexed { i, p ->
                    val x = i * (chartWidth / xCount)
                    val denom = (yMax - yMin).takeIf { it > 0 } ?: 1.0
                    val ratio = (p.price - yMin) / denom
                    val y = chartHeight - (ratio * chartHeight).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, AppColors.ChartLine, style = Stroke(lineStrokeWidth.toPx(), cap = StrokeCap.Round))

                // 绘制数据点
                data.forEachIndexed { i, p ->
                    val x = i * (chartWidth / xCount)
                    val denom = (yMax - yMin).takeIf { it > 0 } ?: 1.0
                    val ratio = (p.price - yMin) / denom
                    val y = chartHeight - (ratio * chartHeight).toFloat()

                    drawCircle(
                        color = AppColors.Accent,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }

                // 高亮选中的数据点
                tapOffset?.let { offset ->
                    drawCircle(
                        color = AppColors.Accent,
                        radius = 10f,
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = offset
                    )
                }
            }

            // 显示选中数据点的价格提示
            selectedPoint?.let { point ->
                tapOffset?.let { offset ->
                    with(density) {
                        Surface(
                            modifier = Modifier
                                .offset(
                                    x = (offset.x + leftAxisSpace.toPx()).toDp() - 60.dp,
                                    y = (offset.y + 16.dp.toPx()).toDp() - 50.dp
                                )
                                .width(120.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.Accent.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
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
        }
    }
}

@Composable
private fun PlatformPriceCardList(
    items: List<PlatformPrice>,
    context: Context,
    onItemClick: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { row ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable {
                        onItemClick(row.link ?: "")
                    },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 平台图标
                        PlatformIcon(platformIcon = row.platformIcon)

                        // 平台信息
                        Column {
                            Text(
                                row.platformName,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.PrimaryText
                            )
                            if (!row.link.isNullOrEmpty()) {
                                Text(
                                    "Tap to open",
                                    fontSize = 12.sp,
                                    color = AppColors.SecondaryText
                                )
                            }
                        }
                    }

                    // 价格
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

// 分享功能 - 使用 Android 系统自带分享
private fun shareProduct(
    context: Context,
    pid: Int,
    name: String,
    price: Double,
    platforms: List<PlatformPrice>
) {
    val bestPlatform = platforms.minByOrNull { it.price }

    // Deep Link - 用户点击后打开你的 App
    val appLink = "dealtracker://product/$pid"

    val shareText = buildString {
        append("Check out this deal!\n\n")
        append("$name\n")
        append("Best Price: $${"%.2f".format(price)}")
        if (bestPlatform != null) {
            append(" from ${bestPlatform.platformName}")
        }
        append("\n\nView in app: $appLink")

        // 如果有购买链接，也附上
        if (!bestPlatform?.link.isNullOrEmpty()) {
            append("\nBuy now: ${bestPlatform.link}")
        }
    }

    // 使用系统分享
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Deal: $name")
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

// 打开URL
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
        android.util.Log.e("ProductDetail", "Suceessed to open URL: $url")
    } catch (e: Exception) {
        android.util.Log.e("ProductDetail", "Failed to open URL: $url", e)
    }
}

// 平台图标组件
@Composable
private fun PlatformIcon(platformIcon: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppColors.Card),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 如果是URL（包含http或https）
            platformIcon.startsWith("http://") || platformIcon.startsWith("https://") -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(platformIcon)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Platform Icon",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            // 如果是drawable资源名称
            platformIcon.isNotEmpty() -> {
                val context = LocalContext.current
                val drawableId = remember(platformIcon) {
                    context.resources.getIdentifier(
                        platformIcon,
                        "drawable",
                        context.packageName
                    )
                }

                if (drawableId != 0) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = "Platform Icon",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 如果找不到drawable，使用默认图标
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = "Platform Icon",
                        modifier = Modifier.size(24.dp),
                        tint = AppColors.SecondaryText
                    )
                }
            }
            // 默认图标
            else -> {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "Platform Icon",
                    modifier = Modifier.size(24.dp),
                    tint = AppColors.SecondaryText
                )
            }
        }
    }
}