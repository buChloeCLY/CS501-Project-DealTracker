package com.example.dealtracker.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dealtracker.domain.UserManager
import com.example.dealtracker.domain.model.WishlistItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    navController: NavController,
    uid: Int = 1,  // 添加默认值
    viewModel: WishlistViewModel = viewModel()
) {
    val currentUser by UserManager.currentUser.collectAsState()
    val isLoggedIn = currentUser != null

    // 使用当前登录用户的 uid，如果没有登录则使用传入的 uid
    val actualUid = currentUser?.uid ?: uid

    // 如果是游客模式，显示提示页面
    if (!isLoggedIn) {
        GuestWishListScreen(navController)
        return
    }

    // 已登录用户的心愿单
    val wishlist by viewModel.wishlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 加载心愿单数据 - 使用 actualUid
    LaunchedEffect(actualUid) {
        viewModel.loadWishlist(actualUid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wishlist", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCE4D6)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error ?: "Unknown error", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadWishlist(uid) }) {
                            Text("Retry")
                        }
                    }
                }

                wishlist.isEmpty() -> {
                    EmptyWishListView(navController)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
 * 游客模式心愿单页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestWishListScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wishlist", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCE4D6)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Login Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Please login to view your wishlist",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.navigate("register") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyWishListView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Your wishlist is empty",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Start adding items you love!",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("deals") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Text("Browse Products")
        }
    }
}

@Composable
fun WishListItemCard(
    item: WishlistItem,
    onUpdate: (Int, Double?) -> Unit,
    onRemove: (Int, Int) -> Unit
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
                // 图片
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
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 商品信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$${String.format("%.2f", item.current_price)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )

                        if (item.target_price != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Target: $${String.format("%.2f", item.target_price)}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (item.price_met && item.savings != null && item.savings > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💰 Save $${String.format("%.2f", item.savings)}!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                        tint = Color(0xFFE53935)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(12.dp))

            // 目标价格输入
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = {
                        targetPrice = it
                        showSaveButton = true
                    },
                    label = { Text("Target Price", fontSize = 12.sp) },
                    placeholder = { Text("Enter target price") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                if (showSaveButton) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = targetPrice.toDoubleOrNull()
                            onUpdate(item.wid, price)
                            showSaveButton = false
                        },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            }
        }
    }
}