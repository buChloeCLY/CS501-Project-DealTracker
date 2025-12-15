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

/**
 * Main application activity, responsible for initialization, theme setup,
 * permission requests, and handling deep links/notifications.
 */
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Stores notification click information
    private var notificationUid: Int = -1
    private var notificationPid: Int = -1

    // Stores the Product ID from a Deep Link
    private var deepLinkPid: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UserPreferences
        UserPreferences.init(this)
        Log.d(TAG, " UserPreferences initialized")

        // Restore user login state from SharedPreferences
        lifecycleScope.launch {
            val savedUser = UserPreferences.getUser()
            if (savedUser != null) {
                UserManager.setUser(savedUser)
                Log.d(TAG, "Restored user from SharedPreferences: uid=${savedUser.uid}")
            } else {
                Log.d(TAG, "No saved user found")
            }
        }

        // Handle notification click
        handleNotificationClick(intent)

        // Handle Deep Link
        handleDeepLink(intent)

        setContent {
            // Collect theme and font scale settings from UserPreferences Flow
            val darkMode by UserPreferences.darkModeFlow.collectAsState()
            val fontScale by UserPreferences.fontScaleFlow.collectAsState()

            CompositionLocalProvider(
                // Apply global font scale
                LocalDensity provides Density(
                    LocalDensity.current.density,
                    fontScale
                )
            ) {
                // Apply theme (updates in real-time with darkMode changes)
                DealTrackerTheme(
                    darkTheme = darkMode,
                    dynamicColor = false // Prevent dynamic color from overriding dark mode setting
                ) {
                    DealTrackerApp(
                        notificationUid = notificationUid,
                        notificationPid = notificationPid,
                        deepLinkPid = deepLinkPid
                    )
                }
            }
        }

        // Request microphone permission for voice search
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * Handles notification click intent, extracts uid and pid, and marks notification as read.
     * @param intent The incoming Intent.
     */
    private fun handleNotificationClick(intent: Intent) {
        val extras = intent.extras
        if (extras != null && extras.getBoolean("notification_clicked", false)) {
            val uid = extras.getInt("notification_uid", -1)
            val pid = extras.getInt("notification_pid", -1)

            if (uid > 0 && pid > 0) {
                Log.d(TAG, "Notification clicked: uid=$uid, pid=$pid")

                // Save information for navigation
                notificationUid = uid
                notificationPid = pid

                // Mark as read
                markNotificationAsRead(uid, pid)
            }
        }
    }

    /**
     * Handles Deep Link Intent to navigate directly to a product detail screen.
     * Supports scheme: dealtracker://product/{pid}
     * @param intent The incoming Intent.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "dealtracker") {
            Log.d(TAG, " Deep Link detected: $data")

            if (data.host == "product") {
                val pid = data.lastPathSegment?.toIntOrNull()
                if (pid != null && pid > 0) {
                    Log.d(TAG, " Deep Link to product: pid=$pid")
                    deepLinkPid = pid
                }
            }
        }
    }

    /**
     * Calls the backend to mark a price alert as read.
     * @param uid User ID.
     * @param pid Product ID.
     */
    private fun markNotificationAsRead(uid: Int, pid: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = WishlistRepository()
            repository.markRead(uid, pid)
                .onSuccess {
                    Log.d(TAG, "Successfully marked as read: pid=$pid")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to mark as read: ${e.message}")
                }
        }
    }

    /**
     * Handles subsequent incoming intents (e.g., when the activity is already running).
     * @param intent The new incoming Intent.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationClick(intent)
        handleDeepLink(intent)
    }
}

/**
 * Root Composable for the application UI.
 * @param notificationUid User ID from notification click.
 * @param notificationPid Product ID from notification click.
 * @param deepLinkPid Product ID from deep link.
 */
@Composable
fun DealTrackerApp(
    notificationUid: Int = -1,
    notificationPid: Int = -1,
    deepLinkPid: Int = -1
) {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // Navigation triggered by notification click
    LaunchedEffect(notificationUid) {
        if (notificationUid > 0 && notificationPid > 0) {
            Log.d("DealTrackerApp", "Navigating to Wishlist: uid=$notificationUid")

            try {
                navController.navigate("wishlist/$notificationUid") {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
            } catch (e: Exception) {
                Log.e("DealTrackerApp", "Navigation failed: ${e.message}")
            }
        }
    }

    // Navigation triggered by Deep Link
    LaunchedEffect(deepLinkPid) {
        if (deepLinkPid > 0) {
            Log.d("DealTrackerApp", " Deep Link navigation to product: pid=$deepLinkPid")

            try {
                navController.navigate("detail/$deepLinkPid") {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
                Log.d("DealTrackerApp", " Navigating to product detail: pid=$deepLinkPid")
            } catch (e: Exception) {
                Log.e("DealTrackerApp", " Deep Link navigation failed: ${e.message}")
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