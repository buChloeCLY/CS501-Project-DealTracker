package com.example.dealtracker.ui.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.navigation.Routes


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    navController: NavController,
    viewModel: WishListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val wishList by viewModel.wishList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wish List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (wishList.isEmpty()) {
            // 空状态界面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You have not added any items to your wish list.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You can start it now.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Routes.HOME) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Go To Products")
                    }
                }
            }
        } else {
            // 收藏页商品列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wishList) { product ->
                    WishListItem(
                        product = product,
                        onTargetPriceChange = { newPrice ->
                            viewModel.updateTargetPrice(product.pid, newPrice)
                        }
                    )
                }
            }
        }
    }
}

// WishListItem

@Composable
fun WishListItem(
    product: Product,
    onTargetPriceChange: (Double) -> Unit
) {
    var targetPrice by remember { mutableStateOf(product.price) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 当前图片为占位符
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = product.title,
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text("Current: ${product.priceText}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target: $", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    TextField(
                        value = "%.2f".format(targetPrice),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let {
                                targetPrice = it
                                onTargetPriceChange(it)
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                }
            }
        }
    }
}
