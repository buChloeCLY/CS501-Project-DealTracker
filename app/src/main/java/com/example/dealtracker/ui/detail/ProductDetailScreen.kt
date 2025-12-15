package com.example.dealtracker.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dealtracker.domain.model.Category
import com.example.dealtracker.domain.model.PlatformPrice
import com.example.dealtracker.ui.detail.viewmodel.ProductViewModel
import com.example.dealtracker.ui.theme.AppTheme
import com.example.dealtracker.domain.model.Platform
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.wishlist.WishListHolder
import com.example.dealtracker.ui.wishlist.viewmodel.WishListViewModel
import com.example.dealtracker.data.remote.repository.HistoryRepository
import kotlinx.coroutines.launch

/**
 * Main Composable for the Product Detail Screen.
 * @param pid Product ID.
 * @param name Product name (title).
 * @param price Current product price.
 * @param rating Product rating.
 * @param navController Navigation controller.
 * @param uid User ID (default 1).
 */
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

    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    val platformPricesState = viewModel.platformPrices.collectAsState()
    val priceHistoryState = viewModel.priceHistory.collectAsState()

    val currentUser by com.example.dealtracker.domain.UserManager.currentUser.collectAsState()
    val actualUid = currentUser?.uid ?: uid

    val wishList by WishListHolder.wishList.collectAsState()
    val isInWishlist = remember(wishList, pid) {
        wishList.any { it.pid == pid }
    }

    // Record viewing history
    LaunchedEffect(pid, currentUser) {
        currentUser?.let { user ->
            scope.launch {
                try {
                    historyRepository.addHistory(user.uid, pid)
                } catch (e: Exception) {
                    android.util.Log.e("ProductDetail", "Failed to record history", e)
                }
            }
        }
    }

    // Load price data
    LaunchedEffect(pid) {
        viewModel.loadPlatformPrices(pid = pid)
        viewModel.loadPriceHistory(pid = pid, days = 30)
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
    val nonEbayPrices = platformPrices.filter { it.platformName != "eBay" }

    val currentPrice = nonEbayPrices.minOfOrNull { it.price } ?: price
    val lowestPlatform = nonEbayPrices.minByOrNull { it.price }

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
                    IconButton(onClick = {
                        shareProduct(context, pid, name, currentPrice, platformPrices)
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share"
                        )
                    }

                    IconButton(onClick = {
                        if (currentUser == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please login first")
                            }
                            navController.navigate("login")
                            return@IconButton
                        }

                        // Create a Product object for wishlist operations
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
                            tint = if (isInWishlist) colors.error else colors.primaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground,
                    titleContentColor = colors.topBarContent,
                    navigationIconContentColor = colors.topBarContent,
                    actionIconContentColor = colors.topBarContent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductHeader(
                name = name,
                currentPrice = currentPrice,
                lowestPlatform = lowestPlatform?.platformName,
                hasData = platformPrices.isNotEmpty()
            )

            // Price History Chart section
            when {
                priceHistory.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }

                priceHistory.error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.card
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No platform data available",
                                color = colors.secondaryText,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                else -> {
                    ScrollablePriceChart(priceHistory = priceHistory.data)
                }
            }

            // Platform Prices List section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val fontScale = AppTheme.fontScale

                    Text(
                        "Available on",
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * fontScale).sp,
                        color = colors.primaryText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (platformPrices.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            platformPrices.forEach { platform ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clickable {
                                            android.util.Log.d(
                                                "ProductDetail",
                                                "Clicked: ${platform.platformName} - ${platform.link}"
                                            )
                                            if (!platform.link.isNullOrEmpty()) {
                                                openUrl(context, platform.link)
                                            } else {
                                                android.util.Log.d(
                                                    "ProductDetail",
                                                    "No link available for ${platform.platformName}"
                                                )
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val fontScale = AppTheme.fontScale
                                            PlatformIcon(platformIcon = platform.platformIcon)
                                            Text(
                                                text = platform.platformName,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = (16 * fontScale).sp,
                                                color = colors.primaryText
                                            )
                                        }
                                        Text(
                                            text = "$${"%.2f".format(platform.price)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (16 * fontScale).sp,
                                            color = colors.accent
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "No platform data available",
                            color = colors.secondaryText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the product name and current lowest price/platform.
 * @param name Product name.
 * @param currentPrice The lowest price.
 * @param lowestPlatform The name of the platform offering the lowest price.
 * @param hasData Indicates if platform price data was successfully loaded.
 */
@Composable
private fun ProductHeader(
    name: String,
    currentPrice: Double,
    lowestPlatform: String?,
    hasData: Boolean
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                name,
                fontWeight = FontWeight.Bold,
                fontSize = (20 * fontScale).sp,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$${"%.2f".format(currentPrice)}",
                fontSize = (28 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
            if (hasData && lowestPlatform != null) {
                Text(
                    "Lowest price on $lowestPlatform",
                    fontSize = (12 * fontScale).sp,
                    color = colors.secondaryText
                )
            }
        }
    }
}

/**
 * Displays a list of cards for platform-specific prices.
 * (Note: Not currently used in ProductDetailScreen, kept for context.)
 */
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.card)
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
                        PlatformIcon(platformIcon = row.platformIcon)

                        Column {
                            Text(
                                row.platformName,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        "$${"%.2f".format(row.price)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Prepares and launches an Intent to share the product details.
 */
private fun shareProduct(
    context: Context,
    pid: Int,
    name: String,
    price: Double,
    platforms: List<PlatformPrice>
) {
    val bestPlatform = platforms.minByOrNull { it.price }
    val appLink = "dealtracker://product/$pid"

    val shareText = buildString {
        append("Check out this deal!\n\n")
        append("$name\n")
        append("Best Price: $${"%.2f".format(price)}")
        if (bestPlatform != null) {
            append(" from ${bestPlatform.platformName}")
        }
        append("\n\nView in app: $appLink")

        if (!bestPlatform?.link.isNullOrEmpty()) {
            append("\nBuy now: ${bestPlatform.link}")
        }
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Deal: $name")
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

/**
 * Opens a URL using Chrome Custom Tabs or a regular browser as a fallback.
 */
private fun openUrl(context: Context, url: String) {
    try {
        // Use Chrome Custom Tabs
        val builder = androidx.browser.customtabs.CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        builder.setUrlBarHidingEnabled(false)

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))

        android.util.Log.d("ProductDetail", "Opened URL in Custom Tab: $url")
    } catch (e: Exception) {
        // Fallback to regular browser
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            android.util.Log.d("ProductDetail", "Opened URL in browser: $url")
        } catch (ex: Exception) {
            android.util.Log.e("ProductDetail", "Failed to open URL: $url", ex)
        }
    }
}

/**
 * Displays the platform icon, loading from URL, local resource, or a default icon.
 * @param platformIcon The URL or resource name for the icon.
 */
@Composable
private fun PlatformIcon(platformIcon: String) {
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.card),
        contentAlignment = Alignment.Center
    ) {
        when {
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
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = "Platform Icon",
                        modifier = Modifier.size(24.dp),
                        tint = colors.secondaryText
                    )
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "Platform Icon",
                    modifier = Modifier.size(24.dp),
                    tint = colors.secondaryText
                )
            }
        }
    }
}