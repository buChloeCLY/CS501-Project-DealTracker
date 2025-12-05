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
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val context = LocalContext.current

    // 自动初始化 SharedPreferences
    UserPreferences.init(context)

    val user = UserPreferences.getUser()
    val userId = user?.uid ?: -1     // ← 自动读取登录用户ID（未登录 = -1）
    // 浏览历史仓库
    val historyRepository = remember { HistoryRepository() }
    // ProductRepository 实现
    val productRepository = remember { ProductRepositoryImpl() }

    // 推荐仓库
    val recommendationRepository = remember {
        RecommendationRepository(
            historyRepository = historyRepository,
            productRepository = productRepository
        )
    }

    // ViewModel（用 Factory 注入 userId）
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            recommendationRepository = recommendationRepository,
            userId = userId
        )
    )

    // ------------------ UI 状态监听 ------------------
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    // 推荐产品
    val recommended by homeViewModel.recommendedProducts.collectAsState()
    var isSearchMode by remember { mutableStateOf(false) }
    /** ---------- 回到顶部按钮滚动控制 ---------- */
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    /** ---------- 语音识别 Launcher ---------- */
    val voiceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val text = result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                    if (text.isNullOrBlank()) {
                        homeViewModel.setVoiceError("Empty voice result")
                    } else {
                        homeViewModel.applyVoiceResult(text)
                    }
                } else {
                    homeViewModel.setVoiceError("Voice recognition canceled")
                }
            } catch (e: Exception) {
                homeViewModel.setVoiceError("Recognition failed: ${e.message}")
            }
        }

    Scaffold(
        floatingActionButton = {
            // 超过 5 个 item 显示按钮
            val showScrollTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 4 }
            }
            AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } }
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Top")
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
                        Text("Home", fontWeight = FontWeight.Bold)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            /** Voice input */
                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                }

                                val pm = context.packageManager
                                val activities = pm.queryIntentActivities(intent, 0)

                                if (activities.isNotEmpty()) {
                                    voiceLauncher.launch(intent)
                                } else {
                                    homeViewModel.setVoiceError("This device does not support speech recognition")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardVoice,
                                    contentDescription = "Voice Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            /** 输入框 */
                            TextField(
                                value = searchQuery,
                                onValueChange = { homeViewModel.updateQuery(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search products...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            navController.navigateToRoot(
                                                Routes.dealsWithQuery(searchQuery)
                                            )
                                        }
                                    }
                                )
                            )

                            /** ✕ 清除按钮（有字时才显示） */
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
                            if (searchQuery.isNotBlank()) {
                                navController.navigateToRoot(Routes.dealsWithQuery(searchQuery))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        /** ---------- 列表内容 ---------- */
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
                    navController.navigate("deals/$category")
                })
            }
            item {
                Text(
                    text = "Deals of the Day",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            /** ---------- 推荐商品列表 ---------- */
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

/* ----------------------------- 分类部分 ----------------------------- */
@Composable
fun CategorySection(onCategoryClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val allCategories = listOf(
        "Electronics", "Beauty", "Home", "Food", "Fashion", "Sports",
        "Books", "Toys", "Health", "Outdoors", "Office", "Pets"
    )
    val displayedCategories = if (expanded) allCategories else allCategories.take(6)

    Column {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                    .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (expanded) "Less ▲" else "More ▼",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/* ----------------------------- 单个分类卡片 ----------------------------- */
@Composable
fun CategoryCard(category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .background(Color(0xFFF2F2F2), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = category, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
}

/* ======================== 单个商品 Item ======================== */

@Composable
fun DealItem(product: Product, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(12.dp))
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
            Text(product.title, fontWeight = FontWeight.SemiBold)
            Text(product.priceText, color = Color(0xFF388E3C))
            Text("Best from ${product.platform}", color = Color.Gray, fontSize = 12.sp)
        }
    }
}