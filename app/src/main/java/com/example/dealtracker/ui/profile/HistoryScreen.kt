package com.example.dealtracker.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter

data class HistoryItem(
    val id: Int,
    val productName: String,
    val price: Double,
    val viewedDate: String,
    val imageUrl: String = ""
)

// Sample data
private val sampleHistory = listOf(
    HistoryItem(1, "iPhone 16 Pro Max", 1199.99, "2 hours ago"),
    HistoryItem(2, "Dyson V15 Vacuum Cleaner", 649.99, "Yesterday"),
    HistoryItem(3, "Sony WH-1000XM5 Headphones", 399.99, "Yesterday"),
    HistoryItem(4, "Apple Watch Series 9", 429.00, "3 days ago"),
    HistoryItem(5, "Samsung Galaxy S24 Ultra", 1299.99, "1 week ago"),
    HistoryItem(6, "MacBook Pro M3", 1999.00, "1 week ago"),
    HistoryItem(7, "Nike Air Max Sneakers", 149.99, "2 weeks ago"),
    HistoryItem(8, "KitchenAid Stand Mixer", 379.99, "2 weeks ago")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController) {
    var historyList by remember { mutableStateOf(sampleHistory) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Browsing History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { historyList = emptyList() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            if (historyList.isEmpty()) {
                EmptyHistoryView(navController)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList) { item ->
                        HistoryItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryView(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = "Empty history",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFBDBDBD)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Browsing History",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Products you view will appear here",
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("home") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EA)
            )
        ) {
            Text("Start Shopping")
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(item.imageUrl),
                        contentDescription = item.productName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Product",
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$${"%.2f".format(item.price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.viewedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}