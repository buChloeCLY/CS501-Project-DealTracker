package com.example.dealtracker.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dealtracker.domain.model.WishlistItem
import com.example.dealtracker.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * 愿望清单页面 - 联动后端 API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    navController: NavController,
    uid: Int = 1 // TODO: 从登录状态获取真实 uid
) {
    val viewModel: WishlistViewModel = viewModel()
    val wishlist by viewModel.wishlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 加载心愿单
    LaunchedEffect(uid) {
        viewModel.loadWishlist(uid)
        viewModel.loadStats(uid)
    }

    // 显示错误
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Wish List",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.PrimaryText
                        )
                        stats?.let {
                            Text(
                                "${it.total_items} items • ${it.items_on_sale} on sale",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.SecondaryText
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.PrimaryText
                        )
                    }
                },
                actions = {
                    // 刷新按钮
                    IconButton(onClick = {
                        viewModel.loadWishlist(uid)
                        viewModel.loadStats(uid)
                    }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AppColors.PrimaryText
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent)
                    }
                }
                wishlist.isEmpty() -> {
                    EmptyWishList(navController)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(wishlist, key = { it.wid }) { item ->
                            WishListItemCard(
                                item = item,
                                onUpdate = { wid, targetPrice ->
                                    viewModel.updateWishlistItem(
                                        uid = uid,
                                        wid = wid,
                                        targetPrice = targetPrice,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Updated successfully")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(error)
                                            }
                                        }
                                    )
                                },
                                onRemove = { pid, wid ->
                                    viewModel.removeFromWishlist(
                                        uid = uid,
                                        pid = pid,
                                        wid = wid,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Removed from wishlist")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(error)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 空收藏列表提示
 */
@Composable
fun EmptyWishList(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = "Empty wishlist",
            modifier = Modifier.size(64.dp),
            tint = AppColors.SecondaryText
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your Wish List is Empty",
            fontWeight = FontWeight.Bold,
            color = AppColors.PrimaryText,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start adding products you love!",
            color = AppColors.SecondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("home") },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent
            )
        ) {
            Text("Browse Products", fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * 单个收藏商品项
 */
@Composable
fun WishListItemCard(
    item: WishlistItem,
    onUpdate: (Int, Double?) -> Unit,
    onRemove: (Int, Int) -> Unit  // (pid, wid)
) {
    var targetPrice by remember {
        mutableStateOf(item.target_price?.toString() ?: "")
    }
    var showSaveButton by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                clip = true
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.price_met) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图片部分
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.image_url.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(item.image_url),
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "No image",
                            tint = AppColors.SecondaryText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 商品信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.PrimaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 当前价格
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Current: ",
                            color = AppColors.SecondaryText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$${"%.2f".format(item.current_price)}",
                            color = if (item.price_met) Color(0xFF4CAF50) else AppColors.PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // 价格达标提示
                    if (item.price_met && item.savings != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "💰 Save ${"%.2f".format(item.savings)}!",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 删除按钮
                IconButton(
                    onClick = { onRemove(item.pid, item.wid) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFFFFE8E6),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(12.dp))

            // 目标价格设置
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Target Price:",
                    color = AppColors.SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(100.dp)
                )

                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            targetPrice = newValue
                            showSaveButton = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Set price alert") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.Outline
                    )
                )

                if (showSaveButton) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = targetPrice.toDoubleOrNull()
                            onUpdate(item.wid, price)
                            showSaveButton = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}