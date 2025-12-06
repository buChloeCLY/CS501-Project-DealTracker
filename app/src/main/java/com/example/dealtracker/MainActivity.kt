package com.example.dealtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dealtracker.data.local.UserPreferences
import com.example.dealtracker.data.remote.repository.WishlistRepository
import com.example.dealtracker.domain.UserManager
import com.example.dealtracker.ui.navigation.*
import com.example.dealtracker.ui.theme.DealTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // 保存通知点击的信息
    private var notificationUid: Int = -1
    private var notificationPid: Int = -1

    // ⭐ 保存 Deep Link 的产品 ID
    private var deepLinkPid: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 第 1 步：初始化 UserPreferences
        UserPreferences.init(this)
        Log.d(TAG, "✅ UserPreferences initialized")

        // 第 2 步：从 SharedPreferences 恢复用户登录状态
        lifecycleScope.launch {
            val savedUser = UserPreferences.getUser()
            if (savedUser != null) {
                UserManager.setUser(savedUser)
                Log.d(TAG, "✅ Restored user from SharedPreferences: uid=${savedUser.uid}")
            } else {
                Log.d(TAG, "⚠️ No saved user found")
            }
        }

        // 第 3 步：处理通知点击
        handleNotificationClick(intent)

        // ⭐ 第 4 步：处理 Deep Link
        handleDeepLink(intent)

        setContent {
            // 使用 Flow → Compose 自动监听并刷新 UI
            val darkMode by UserPreferences.darkModeFlow.collectAsState()
            val fontScale by UserPreferences.fontScaleFlow.collectAsState()

            CompositionLocalProvider(
                // 应用全局字体缩放
                LocalDensity provides Density(
                    LocalDensity.current.density,
                    fontScale
                )
            ) {
                // 应用主题（darkMode 实时变化）
                DealTrackerTheme(
                    darkTheme = darkMode,
                    dynamicColor = false // 避免动态色覆盖 darkMode 设置
                ) {
                    DealTrackerApp(
                        notificationUid = notificationUid,
                        notificationPid = notificationPid,
                        deepLinkPid = deepLinkPid
                    )
                }
            }
        }

        // 请求麦克风权限
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * 处理通知点击
     */
    private fun handleNotificationClick(intent: Intent) {
        val extras = intent.extras
        if (extras != null && extras.getBoolean("notification_clicked", false)) {
            val uid = extras.getInt("notification_uid", -1)
            val pid = extras.getInt("notification_pid", -1)

            if (uid > 0 && pid > 0) {
                Log.d(TAG, "✅ Notification clicked: uid=$uid, pid=$pid")

                // 保存信息用于导航
                notificationUid = uid
                notificationPid = pid

                // 标记为已读
                markNotificationAsRead(uid, pid)
            }
        }
    }

    /**
     * ⭐ 处理 Deep Link
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "dealtracker") {
            Log.d(TAG, "✅ Deep Link detected: $data")

            if (data.host == "product") {
                val pid = data.lastPathSegment?.toIntOrNull()
                if (pid != null && pid > 0) {
                    Log.d(TAG, "✅ Deep Link to product: pid=$pid")
                    deepLinkPid = pid
                }
            }
        }
    }

    private fun markNotificationAsRead(uid: Int, pid: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = WishlistRepository()
            repository.markRead(uid, pid)
                .onSuccess {
                    Log.d(TAG, "✅ Successfully marked as read: pid=$pid")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to mark as read: ${e.message}")
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationClick(intent)
        handleDeepLink(intent)
    }
}

@Composable
fun DealTrackerApp(
    notificationUid: Int = -1,
    notificationPid: Int = -1,
    deepLinkPid: Int = -1
) {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // 通知点击导航
    LaunchedEffect(notificationUid) {
        if (notificationUid > 0 && notificationPid > 0) {
            Log.d("DealTrackerApp", "🔔 Navigating to Wishlist: uid=$notificationUid")

            try {
                navController.navigate("wishlist/$notificationUid") {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
            } catch (e: Exception) {
                Log.e("DealTrackerApp", "Navigation failed: ${e.message}")
            }
        }
    }

    // ⭐ Deep Link 导航
    LaunchedEffect(deepLinkPid) {
        if (deepLinkPid > 0) {
            Log.d("DealTrackerApp", "🔗 Deep Link navigation to product: pid=$deepLinkPid")

            try {
                navController.navigate("detail/$deepLinkPid") {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
                Log.d("DealTrackerApp", "✅ Navigating to product detail: pid=$deepLinkPid")
            } catch (e: Exception) {
                Log.e("DealTrackerApp", "❌ Deep Link navigation failed: ${e.message}")
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBarRouteAware(
                currentRoute = currentRoute,
                onTabSelected = { route ->
                    navController.navigateToRoot(route)
                }
            )
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}