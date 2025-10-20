package com.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DealsApp()
        }
    }
}

@Composable
fun DealsApp() {
    // 创建导航控制器
    val navController = rememberNavController()

    // 应用主题容器（Surface 可选，提供背景色）
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // 调用导航图，控制页面跳转
            MainNavGraph(navController = navController)
        }
    }
}
