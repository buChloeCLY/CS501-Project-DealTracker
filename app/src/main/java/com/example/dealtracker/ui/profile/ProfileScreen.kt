package com.example.dealtracker.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.dealtracker.domain.UserManager
import com.example.dealtracker.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    // 初始化测试用户数据
    LaunchedEffect(Unit) {
        if (UserManager.getUser() == null) {
            UserManager.setUser(
                User(
                    uid = 1,
                    name = "John Doe",
                    email = "john.doe@email.com",
                    gender = "Male"
                )
            )
        }
    }

    val currentUser by UserManager.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF6B6B6B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCE4D6)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Avatar and Name
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFE0E0E0), CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF9E9E9E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentUser?.name ?: "Guest",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Text(
                text = currentUser?.email ?: "Not logged in",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Menu Items
            ProfileMenuItem(
                icon = Icons.Default.List,
                title = "Lists",
                onClick = { navController.navigate("wishlist") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileMenuItem(
                icon = Icons.Default.History,
                title = "History",
                onClick = { navController.navigate("history") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Setting",
                onClick = { navController.navigate("settings") }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF616161),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}