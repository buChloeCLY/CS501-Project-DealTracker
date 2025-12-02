package com.example.dealtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dealtracker.ui.navigation.*
import com.example.dealtracker.ui.theme.DealTrackerTheme
import com.example.dealtracker.data.local.UserPreferences

class MainActivity : ComponentActivity() {

    // 请求麦克风权限
    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 可选处理结果 */ }

    // ⭐ 新增：请求通知权限（Android 13+）
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // 这里可以根据 isGranted 做一些提示（可选）
            // if (!isGranted) { Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserPreferences.init(this)
        // 自动请求麦克风权限
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        // ⭐ 自动请求通知权限（仅在 Android 13+ 时请求）
        askNotificationPermission()

        setContent {
            DealTrackerTheme {
                DealTrackerApp()
            }
        }
    }

    /**
     * Android 13 及以上需要显式请求 POST_NOTIFICATIONS 权限
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun DealTrackerApp() {

    // 创建导航控制器
    val navController = rememberNavController()

    // 监听导航变化，用于隐藏/显示 BottomBar
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

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
        // 主导航图
        MainNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
