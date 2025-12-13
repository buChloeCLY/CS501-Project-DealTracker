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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.navigation.Routes
import com.example.dealtracker.ui.notifications.NotificationHelper
import com.example.dealtracker.ui.theme.AppTheme
import com.example.dealtracker.ui.wishlist.viewmodel.WishListViewModel

// User wishlist screen with price alerts
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    navController: NavController,
    currentUserId: Int,
    viewModel: WishListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale
    val wishList by viewModel.wishList.collectAsState()
    val targetPrices by viewModel.targetPrices.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentUserId) {
        if (currentUserId > 0) {
            Log.d("WishListScreen", "Loading wishlist for uid=$currentUserId")
            viewModel.loadWishlist(currentUserId)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("WishListScreen", "Notification permission result: $isGranted")

        if (isGranted) {
            Toast.makeText(
                context,
                "Notification enabled! You'll receive price alerts.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Notification disabled. Enable in Settings for price alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("WishListScreen", "Notification permission status: $hasPermission")
            return hasPermission
        }
        return true
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("WishListScreen", "Requesting notification permission...")
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                title = {
                    Text(
                        "Wishlist",
                        fontSize = (20 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground,
                    titleContentColor = colors.topBarContent,
                    navigationIconContentColor = colors.topBarContent
                )
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (wishList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Your wishlist is empty",
                        color = colors.secondaryText,
                        fontSize = (16 * fontScale).sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wishList, key = { it.pid }) { product ->
                        val currentTargetPrice = targetPrices[product.pid]

                        WishListItem(
                            product = product,
                            targetPrice = currentTargetPrice,
                            onRemove = {
                                viewModel.removeProduct(currentUserId, product.pid)
                            },
                            onTargetPriceConfirm = { targetPrice ->
                                Log.d("WishListScreen", "Save button clicked: targetPrice=$targetPrice")

                                val hasPermission = checkNotificationPermission()

                                if (!hasPermission) {
                                    Toast.makeText(
                                        context,
                                        "Notification permission needed for price alerts!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    requestNotificationPermission()
                                }

                                viewModel.updateTargetPrice(
                                    uid = currentUserId,
                                    pid = product.pid,
                                    targetPrice = targetPrice,
                                    alertEnabled = true,
                                    onSuccess = { priceReached ->
                                        Toast.makeText(
                                            context,
                                            "Target price saved: $$targetPrice",
                                            Toast.LENGTH_SHORT
                                        ).show()

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
                                                    "Price already reached! Check your notifications.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Log.d("WishListScreen", "Price reached but no permission, notification skipped")
                                                Toast.makeText(
                                                    context,
                                                    "Enable notifications to receive price alerts!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            },
                            onClick = {
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

// Individual wishlist item with target price
@Composable
private fun WishListItem(
    product: Product,
    targetPrice: Double?,
    onRemove: () -> Unit,
    onTargetPriceConfirm: (Double) -> Unit,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale
    var targetPriceInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
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
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current price: $${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = (14 * fontScale).sp,
                        color = colors.secondaryText
                    )

                    if (targetPrice != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val priceReached = product.price <= targetPrice
                        Text(
                            text = "Target price: $${"%.2f".format(targetPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = (14 * fontScale).sp,
                            color = if (priceReached) colors.success else colors.warning,
                            fontWeight = if (priceReached) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = colors.error
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
                            targetPriceInput = ""
                            error = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent
                    )
                ) {
                    Text(if (targetPrice != null) "Update" else "Save")
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error ?: "",
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = (12 * fontScale).sp
                )
            }
        }
    }
}