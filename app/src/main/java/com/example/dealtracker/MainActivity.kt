package com.example.dealtracker

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.dealtracker.ui.auth.LoginScreen
import com.example.dealtracker.ui.auth.RegisterScreen


class MainActivity : ComponentActivity() {

    // 请求麦克风权限
    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

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

        setContent {
            DealTrackerTheme {
                DealTrackerApp()
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
