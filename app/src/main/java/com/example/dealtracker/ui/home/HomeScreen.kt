package com.example.dealtracker.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.dealtracker.data.local.UserPreferences
import com.example.dealtracker.data.remote.repository.HistoryRepository
import com.example.dealtracker.data.remote.repository.ProductRepositoryImpl
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.repository.RecommendationRepository
import com.example.dealtracker.ui.home.viewmodel.HomeViewModel
import com.example.dealtracker.ui.home.viewmodel.HomeViewModelFactory
import com.example.dealtracker.ui.navigation.Routes
import com.example.dealtracker.ui.navigation.navigateToRoot
import com.example.dealtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    // for permission + settings
    val activity = context as? Activity
    val appSettingsIntent = remember {
        Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        )
    }

    UserPreferences.init(context)

    val user = UserPreferences.getUser()
    val userId = user?.uid ?: -1
    val historyRepository = remember { HistoryRepository() }
    val productRepository = remember { ProductRepositoryImpl() }

    val recommendationRepository = remember {
        RecommendationRepository(
            historyRepository = historyRepository,
            productRepository = productRepository
        )
    }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            recommendationRepository = recommendationRepository,
            userId = userId
        )
    )

    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val recommended by homeViewModel.recommendedProducts.collectAsState()
    val voiceError by homeViewModel.voiceError.collectAsState()
    var isSearchMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    // prevent rapid multiple launches
    var isListening by remember { mutableStateOf(false) }
    //  cached intent + support check
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
        }
    }

    fun deviceSupportsSpeech(): Boolean {
        return context.packageManager.queryIntentActivities(speechIntent, 0).isNotEmpty()
    }

    //  show snackbar when voiceError changes
    LaunchedEffect(voiceError) {
        val msg = voiceError ?: return@LaunchedEffect
        val actionLabel = when {
            msg.contains("permission", ignoreCase = true) -> "Settings"
            msg.contains("support", ignoreCase = true) -> null
            msg.contains("failed", ignoreCase = true) -> "Retry"
            else -> null
        }

        val res = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = actionLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )

        if (res == SnackbarResult.ActionPerformed) {
            when (actionLabel) {
                "Settings" -> runCatching { context.startActivity(appSettingsIntent) }
                "Retry" -> {
                }
            }
        }
        homeViewModel.clearVoiceError()
    }

    // permission launcher
    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Check device support and enable voice after obtaining permission
                if (!deviceSupportsSpeech()) {
                    homeViewModel.setVoiceError("This device does not support speech recognition")
                    return@rememberLauncherForActivityResult
                }
                runCatching {
                    isListening = true
                }.onFailure {
                    isListening = false
                    homeViewModel.setVoiceError("Voice start failed: ${it.message ?: "unknown"}")
                }
            } else {
                // Refusal: divided into "ordinary refusal" and "permanent refusal"
                val permanentlyDenied = activity != null &&
                        !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            android.Manifest.permission.RECORD_AUDIO
                        )
                if (permanentlyDenied) {
                    homeViewModel.setVoiceError("Microphone permission denied. Please enable it in Settings.")
                } else {
                    homeViewModel.setVoiceError("Microphone permission is required for voice search.")
                }
            }
        }
    // handle result safely
    val voiceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isListening = false
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val text = result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()

                    if (text.isNullOrBlank()) {
                        homeViewModel.setVoiceError("No speech detected. Please try again.")
                    } else {
                        homeViewModel.applyVoiceResult(text)
                    }
                } else {
                    homeViewModel.setVoiceError("Voice recognition canceled.")
                }
            } catch (e: Exception) {
                homeViewModel.setVoiceError("Recognition failed: ${e.message ?: "unknown error"}")
            }
        }

    //  start voice with all checks
    fun startVoiceSearch() {
        if (isListening) return

        // Supportive check
        if (!deviceSupportsSpeech()) {
            homeViewModel.setVoiceError("This device does not support speech recognition")
            return
        }
        // Permission check
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!granted) {
            // Trigger permission requests
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }

        // Start voice
        runCatching {
            isListening = true
            voiceLauncher.launch(speechIntent)
        }.onFailure { e ->
            isListening = false
            homeViewModel.setVoiceError("Voice start failed: ${e.message ?: "unknown"}")
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val showScrollTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 4 }
            }
            AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = colors.accent
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Top",
                        tint = colors.onPrimary
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isSearchMode) {
                        IconButton(onClick = {
                            isSearchMode = false
                            homeViewModel.updateQuery("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Exit search"
                            )
                        }
                    }
                },
                title = {
                    if (!isSearchMode) {
                        Text(
                            "Home",
                            fontWeight = FontWeight.Bold,
                            fontSize = (20 * fontScale).sp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // ---- UPDATED: voice button uses safe startVoiceSearch() ----
                            IconButton(
                                onClick = { startVoiceSearch() },
                                enabled = !isListening
                            ) {
                                if (isListening) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardVoice,
                                        contentDescription = "Voice Search",
                                        tint = colors.accent
                                    )
                                }
                            }

                            TextField(
                                value = searchQuery,
                                onValueChange = { homeViewModel.updateQuery(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "Search products...",
                                        fontSize = (14 * fontScale).sp
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSearch = {
                                        val q = searchQuery.trim()
                                        if (q.isNotBlank()) {
                                            navController.navigate(Routes.dealsWithQuery(q)) {
                                                popUpTo(Routes.DEALS) { inclusive = true }
                                            }
                                            homeViewModel.updateQuery("")
                                        }
                                    }
                                )
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { homeViewModel.updateQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text"
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (!isSearchMode) {
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Enter search mode"
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            val q = searchQuery.trim()
                            if (q.isNotBlank()) {
                                navController.navigate(Routes.dealsWithQuery(q)) {
                                    popUpTo(Routes.DEALS) { inclusive = true }
                                }
                                homeViewModel.updateQuery("")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
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
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CategorySection(onCategoryClick = { category ->
                    navController.navigate(Routes.dealsWithCategory(category))
                })
            }
            item {
                Text(
                    text = "Deals of the Day",
                    fontSize = (22 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            }
            items(recommended) { product ->
                DealItem(
                    product = product,
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

@Composable
fun CategorySection(onCategoryClick: (String) -> Unit) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale
    var expanded by remember { mutableStateOf(false) }

    val allCategories = listOf(
        "Electronics", "Beauty", "Home", "Food", "Fashion", "Sports",
        "Books", "Toys", "Health", "Pets"
    )
    val displayedCategories = if (expanded) allCategories else allCategories.take(6)

    Column {
        Text(
            text = "Categories",
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        displayedCategories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    CategoryCard(
                        category = category,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick(category) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp)
                    .background(colors.card, shape = RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (expanded) "Less ▲" else "More ▼",
                    fontWeight = FontWeight.Medium,
                    fontSize = (16 * fontScale).sp,
                    color = colors.primaryText
                )
            }
        }
    }
}

@Composable
fun CategoryCard(category: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Box(
        modifier = modifier
            .height(80.dp)
            .background(colors.card, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * fontScale).sp,
            color = colors.primaryText
        )
    }
}

@Composable
fun DealItem(product: Product, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 110.dp)
            .background(colors.card, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                product.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = (14 * fontScale).sp,
                color = colors.primaryText,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                product.priceText,
                color = colors.accent,
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Best from ${product.platform}",
                color = colors.secondaryText,
                fontSize = (12 * fontScale).sp
            )
        }
    }
}