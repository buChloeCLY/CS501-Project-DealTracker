package com.example.dealtracker.ui.detail

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.dealtracker.data.remote.repository.ProductRepositoryImpl
import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.launch

/**
 * Product Detail Screen that loads product data using a Product ID (pid).
 * Used for Deep Links and notification clicks.
 * @param pid The ID of the product to load.
 * @param navController Navigation controller for screen navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreenWithPid(
    pid: Int,
    navController: NavHostController
) {
    val TAG = "ProductDetailWithPid"

    // State management
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = remember { ProductRepositoryImpl() }
    val scope = rememberCoroutineScope()

    // Load product information from API
    LaunchedEffect(pid) {
        Log.d(TAG, "Loading product with pid=$pid")
        isLoading = true
        errorMessage = null

        scope.launch {
            repository.getProductById(pid)
                .onSuccess { loadedProduct ->
                    Log.d(TAG, "Product loaded: ${loadedProduct.title}")
                    product = loadedProduct
                    isLoading = false
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load product: ${error.message}")
                    errorMessage = error.message ?: "Failed to load product"
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isLoading -> "Loading..."
                            product != null -> product!!.title
                            else -> "Product"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCE4D6)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Loading state
                isLoading -> {
                    LoadingView()
                }

                // Error state
                errorMessage != null -> {
                    ErrorView(
                        errorMessage = errorMessage!!,
                        onRetry = {
                            // Retry loading
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                repository.getProductById(pid)
                                    .onSuccess { loadedProduct ->
                                        product = loadedProduct
                                        isLoading = false
                                    }
                                    .onFailure { error ->
                                        errorMessage = error.message
                                        isLoading = false
                                    }
                            }
                        }
                    )
                }

                // Success state - Reuse the main detail screen component
                product != null -> {
                    ProductDetailScreen(
                        pid = product!!.pid,
                        name = product!!.title,
                        price = product!!.price,
                        rating = product!!.rating.toFloat(),
                        navController = navController
                    )
                }
            }
        }
    }
}

/**
 * Displays the loading indicator view.
 */
@Composable
private fun LoadingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator(
            color = Color(0xFFFF6B35),
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading product...",
            fontSize = 16.sp,
            color = Color(0xFF757575)
        )
    }
}

/**
 * Displays the error message and a retry button.
 * @param errorMessage The message to display.
 * @param onRetry Callback function to execute when retry button is clicked.
 */
@Composable
private fun ErrorView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE53935)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            fontSize = 16.sp,
            color = Color(0xFF757575),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Text("Retry", fontSize = 16.sp)
        }
    }
}