package com.example.dealtracker.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.ui.theme.AppColors


/**
 * 愿望清单页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(navController: NavController) {
    val wishList by WishListHolder.wishList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Wish List",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PrimaryText
                    )
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
                    if (wishList.isNotEmpty()) {
                        IconButton(onClick = { WishListHolder.clear() }) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = AppColors.Accent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AppColors.PrimaryText
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
        ) {
            if (wishList.isEmpty()) {
                EmptyWishList(navController)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(wishList, key = { it.pid }) { product ->
                        WishListItem(
                            product = product,
                            onRemove = { WishListHolder.remove(it.pid) }
                        )
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
            Icons.Default.Image,
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
fun WishListItem(
    product: Product,
    onRemove: (Product) -> Unit
) {
    var targetPrice by remember { mutableStateOf(product.price.toString()) }

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
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                if (product.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(product.imageUrl),
                        contentDescription = product.title,
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

            // 商品信息和价格部分
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                // 商品标题
                Text(
                    product.title,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 当前价格
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Current: ",
                        color = AppColors.SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "$${"%.2f".format(product.price)}",
                        color = AppColors.PrimaryText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 目标价格输入
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Target Price:",
                        color = AppColors.SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 目标价格输入框
                    OutlinedTextField(
                        value = targetPrice,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                targetPrice = newValue
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = AppColors.PrimaryText,
                            fontWeight = FontWeight.Medium
                        ),
                        placeholder = {
                            Text(
                                "Set target",
                                color = AppColors.SecondaryText
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Accent,
                            unfocusedBorderColor = AppColors.Outline,
                            cursorColor = AppColors.Accent,
                            focusedTextColor = AppColors.PrimaryText,
                            unfocusedTextColor = AppColors.PrimaryText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 删除按钮
            IconButton(
                onClick = { onRemove(product) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFFFE8E6),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from wishlist",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}