package com.example.dealtracker.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.dealtracker.ui.home.viewmodel.HomeViewModel
import com.example.dealtracker.ui.navigation.Routes
import com.example.dealtracker.ui.navigation.navigateToRoot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, homeViewModel: HomeViewModel = viewModel()) {

    // 监听 ViewModel 的 StateFlow
    val searchQuery by homeViewModel.searchQuery.collectAsState()

    // 控制是否进入搜索模式
    var isSearchMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

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


    /** ---------- 回到顶部按钮滚动控制 ---------- */
    val listState = rememberLazyListState()
    val showToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

// 用于点击事件中启动协程
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            if (showToTop) {
                FloatingActionButton(
                    onClick = {
                        // 在点击事件中使用 coroutineScope.launch
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Top", color = Color.White)
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

                            /** 🎤 Voice input */
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
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
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
                DealsOfTheDaySection(navController = navController)
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

/* ----------------------------- Deals Of The Day ----------------------------- */
@Composable
fun DealsOfTheDaySection(navController: NavHostController) {
    Text(
        text = "Deals of the Day",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    val deals = listOf(
        Triple("iPhone 16", "$999", "Amazon"),
        Triple("Dyson Hair Dryer", "$399", "BestBuy"),
        Triple("Sony Headphones", "$249", "Walmart"),
        Triple("Nike Running Shoes", "$120", "Nike"),
        Triple("Apple Watch", "$349", "Target"),
        Triple("Samsung TV 65", "$799", "BestBuy"),
        Triple("MacBook Air M3", "$1199", "Apple"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        deals.forEach { (name, price, site) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp)
                    .clickable {
                        if (name == "iPhone 16") {
                            navController.navigate(
                                Routes.detailRoute(
                                    pid = 1,
                                    name = name,
                                    price = price.removePrefix("$").toDouble(),
                                    rating = 4.8f
                                )
                            )
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFDDEEE0), RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(price, color = Color(0xFF388E3C), fontSize = 15.sp)
                    Text(
                        "Available on $site",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
