package com.example.dealtracker.ui.wishlist

import androidx.compose.foundation.background
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
import androidx.navigation.NavController
import com.example.dealtracker.domain.model.Product
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
    val context = LocalContext.current

    // 打开页面时检查是否有降价提醒
    LaunchedEffect(currentUserId) {
        viewModel.checkAlerts(currentUserId) { alerts ->
            alerts.forEach { alert ->
                val title = alert.short_title ?: alert.title ?: "Wishlist item"
                val current = alert.current_price ?: return@forEach
                val target = alert.target_price ?: return@forEach

                NotificationHelper.showPriceDropNotification(
                    context = context,
                    pid = alert.pid,
                    title = title,
                    currentPrice = current,
                    targetPrice = target
                )
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
                        WishListItem(
                            product = product,
                            onRemove = {
                                viewModel.removeProduct(currentUserId, product.pid)
                            },
                            onTargetPriceConfirm = { targetPrice ->
                                viewModel.updateTargetPrice(
                                    uid = currentUserId,
                                    pid = product.pid,
                                    targetPrice = targetPrice
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
    onRemove: () -> Unit,
    onTargetPriceConfirm: (Double) -> Unit
) {
    var targetPrice by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    value = targetPrice,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            targetPrice = newValue
                            error = null
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Target price") },
                    isError = error != null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val v = targetPrice.toDoubleOrNull()
                        if (v == null || v <= 0.0) {
                            error = "Invalid price"
                        } else {
                            onTargetPriceConfirm(v)
                        }
                    }
                ) {
                    Text("Save")
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
