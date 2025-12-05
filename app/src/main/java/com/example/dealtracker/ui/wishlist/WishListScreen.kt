package com.example.dealtracker.ui.wishlist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.navigation.Routes
import com.example.dealtracker.ui.notifications.NotificationHelper
import com.example.dealtracker.ui.wishlist.viewmodel.WishListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    navController: NavController,
    currentUserId: Int,
    viewModel: WishListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val wishList by viewModel.wishList.collectAsState()
    val targetPrices by viewModel.targetPrices.collectAsState()  // ⭐ 添加
    val context = LocalContext.current

    // ⭐ 加载 Wishlist
    LaunchedEffect(currentUserId) {
        if (currentUserId > 0) {
            Log.d("WishListScreen", "🔄 Loading wishlist for uid=$currentUserId")
            viewModel.loadWishlist(currentUserId)
        }
    }

    // ⭐ 添加权限请求 Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("WishListScreen", "📱 Notification permission result: $isGranted")

        if (isGranted) {
            Toast.makeText(
                context,
                "✅ Notification enabled! You'll receive price alerts.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "⚠️ Notification disabled. Enable in Settings for price alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ⭐ 检查通知权限
    fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("WishListScreen", "📱 Notification permission status: $hasPermission")
            return hasPermission
        }
        return true // Android 12 及以下不需要运行时权限
    }

    // ⭐ 请求通知权限
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("WishListScreen", "⚠️ Requesting notification permission...")
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 打开页面时检查是否有降价提醒
    LaunchedEffect(currentUserId) {
        if (currentUserId > 0) {
            viewModel.checkAlerts(currentUserId) { alerts ->
                alerts.forEach { alert ->
                    val title = alert.short_title ?: alert.title ?: "Wishlist item"
                    val current = alert.current_price ?: return@forEach
                    val target = alert.target_price ?: return@forEach

                    NotificationHelper.showPriceDropNotification(
                        context = context,
                        uid = currentUserId,
                        pid = alert.pid,
                        title = title,
                        currentPrice = current,
                        targetPrice = target
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlist") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(innerPadding)
        ) {
            if (wishList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Your wishlist is empty")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wishList, key = { it.pid }) { product ->
                        // ⭐ 获取当前商品的 target price
                        val currentTargetPrice = targetPrices[product.pid]

                        WishListItem(
                            product = product,
                            targetPrice = currentTargetPrice,  // ⭐ 传递 target price
                            onRemove = {
                                viewModel.removeProduct(currentUserId, product.pid)
                            },
                            onTargetPriceConfirm = { targetPrice ->
                                Log.d("WishListScreen", "💾 Save button clicked: targetPrice=$targetPrice")

                                // ⭐ 第一步：检查权限
                                val hasPermission = checkNotificationPermission()

                                if (!hasPermission) {
                                    // ⭐ 没有权限，显示提示并请求
                                    Toast.makeText(
                                        context,
                                        "⚠️ Notification permission needed for price alerts!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    requestNotificationPermission()
                                }

                                // ⭐ 第二步：保存 target_price（无论权限状态）
                                viewModel.updateTargetPrice(
                                    uid = currentUserId,
                                    pid = product.pid,
                                    targetPrice = targetPrice,
                                    alertEnabled = true,
                                    onSuccess = { priceReached ->
                                        Toast.makeText(
                                            context,
                                            "✅ Target price saved: $$targetPrice",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // ⭐ 第三步：如果价格已达标且有权限，显示通知
                                        if (priceReached) {
                                            if (hasPermission) {
                                                NotificationHelper.showPriceDropNotification(
                                                    context = context,
                                                    uid = currentUserId,
                                                    pid = product.pid,
                                                    title = product.title,
                                                    currentPrice = product.price,
                                                    targetPrice = targetPrice
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "🎉 Price already reached! Check your notifications.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Log.d("WishListScreen", "⚠️ Price reached but no permission, notification skipped")
                                                Toast.makeText(
                                                    context,
                                                    "💡 Enable notifications to receive price alerts!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            },
                            onClick = {
                                // ⭐ 点击商品跳转到详情页
                                navController.navigate(
                                    Routes.detailRoute(
                                        pid = product.pid,
                                        name = product.title,
                                        price = product.price,
                                        rating = product.rating
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WishListItem(
    product: Product,
    targetPrice: Double?,  // ⭐ 添加参数
    onRemove: () -> Unit,
    onTargetPriceConfirm: (Double) -> Unit,
    onClick: () -> Unit  // ⭐ 添加点击回调
) {
    var targetPriceInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },  // ⭐ 添加点击事件
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current price: $${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555)
                    )

                    // ⭐ 显示 Target Price
                    if (targetPrice != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val priceReached = product.price <= targetPrice
                        Text(
                            text = "Target price: $${"%.2f".format(targetPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (priceReached) Color(0xFF2E7D32) else Color(0xFFF57C00),
                            fontWeight = if (priceReached) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = targetPriceInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            targetPriceInput = newValue
                            error = null
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Target price") },
                    placeholder = {
                        Text(
                            if (targetPrice != null)
                                "Current: $${"%.2f".format(targetPrice)}"
                            else
                                "e.g., 45.99"
                        )
                    },
                    isError = error != null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val v = targetPriceInput.toDoubleOrNull()
                        if (v == null || v <= 0.0) {
                            error = "Invalid price"
                        } else {
                            onTargetPriceConfirm(v)
                            targetPriceInput = ""  // 清空输入框
                            error = null
                        }
                    }
                ) {
                    Text(if (targetPrice != null) "Update" else "Save")
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}