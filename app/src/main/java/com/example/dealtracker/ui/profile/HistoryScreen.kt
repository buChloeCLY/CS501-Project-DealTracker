package com.example.dealtracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.dealtracker.domain.UserManager
import com.example.dealtracker.domain.model.History
import com.example.dealtracker.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

// User browsing history screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: HistoryViewModel = viewModel()
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale
    val currentUser by UserManager.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            viewModel.loadHistory(user.uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Browsing History",
                        fontWeight = FontWeight.Bold,
                        fontSize = (20 * fontScale).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.histories.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Text(
                                "Clear All",
                                color = colors.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
        containerColor = colors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.accent
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading history",
                            color = colors.error,
                            fontSize = (16 * fontScale).sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            color = colors.secondaryText,
                            fontSize = (14 * fontScale).sp
                        )
                    }
                }

                uiState.histories.isEmpty() -> {
                    EmptyHistoryView()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.histories,
                            key = { it.hid }
                        ) { history ->
                            HistoryItem(
                                history = history,
                                onItemClick = {
                                    navController.navigate("detail/${history.pid}")
                                },
                                onDeleteClick = {
                                    currentUser?.let { user ->
                                        viewModel.deleteHistory(history.hid, user.uid)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to clear all browsing history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentUser?.let { user ->
                            viewModel.clearAllHistory(user.uid)
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

// Individual history item card
@Composable
fun HistoryItem(
    history: History,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = history.productImage,
                contentDescription = history.productTitle,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.card),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = history.productTitle,
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$${String.format("%.2f", history.productPrice)}",
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = history.productPlatform,
                        fontSize = (12 * fontScale).sp,
                        color = colors.secondaryText
                    )

                    Text(
                        text = " • ",
                        fontSize = (12 * fontScale).sp,
                        color = colors.secondaryText
                    )

                    Text(
                        text = formatTimestamp(history.viewedAt),
                        fontSize = (12 * fontScale).sp,
                        color = colors.secondaryText
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = colors.tertiaryText
                )
            }
        }
    }
}

// Empty state view
@Composable
fun EmptyHistoryView() {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No browsing history",
            fontSize = (18 * fontScale).sp,
            fontWeight = FontWeight.Medium,
            color = colors.secondaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Products you view will appear here",
            fontSize = (14 * fontScale).sp,
            color = colors.tertiaryText
        )
    }
}

// Format timestamp to relative time
private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(timestamp)

        val now = Date()
        val diff = now.time - (date?.time ?: 0)

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    } catch (e: Exception) {
        timestamp
    }
}