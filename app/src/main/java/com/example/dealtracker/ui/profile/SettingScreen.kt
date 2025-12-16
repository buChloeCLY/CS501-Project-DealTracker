package com.example.dealtracker.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.dealtracker.ui.theme.AppTheme
import com.example.dealtracker.ui.theme.FontSize
import com.example.dealtracker.ui.theme.FontSizeManager
import com.example.dealtracker.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    var selectedFontSize by remember { mutableStateOf(FontSizeManager.getFontSize(context)) }
    val isDarkMode by ThemeManager.isDarkMode(context).collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = (20 * fontScale).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground,
                    titleContentColor = colors.topBarContent,
                    navigationIconContentColor = colors.topBarContent
                )
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    onClick = { navController.navigate("edit_profile") }
                )
            }

            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Size",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Font Size",
                                fontSize = (16 * fontScale).sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primaryText
                            )
                            Text(
                                text = selectedFontSize.displayName,
                                fontSize = (13 * fontScale).sp,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FontSize.values().forEach { fontSize ->
                            FontSizeOption(
                                fontSize = fontSize,
                                isSelected = selectedFontSize == fontSize,
                                onClick = {
                                    selectedFontSize = fontSize
                                    FontSizeManager.setFontSize(context, fontSize)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Enable dark theme",
                    checked = isDarkMode,
                    onCheckedChange = { enabled ->
                        ThemeManager.setDarkMode(context, enabled)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FontSizeOption(
    fontSize: FontSize,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = colors.accent,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.accent.copy(alpha = 0.1f) else colors.card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "A",
                fontSize = (16 * fontSize.scale).sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) colors.accent else colors.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fontSize.displayName,
                fontSize = (12 * fontScale).sp,
                color = if (isSelected) colors.accent else colors.secondaryText
            )
        }
    }
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = colors.secondaryText,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText
            )
            Text(
                text = subtitle,
                fontSize = (13 * fontScale).sp,
                color = colors.secondaryText
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = colors.tertiaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = colors.secondaryText,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText
            )
            Text(
                text = subtitle,
                fontSize = (13 * fontScale).sp,
                color = colors.secondaryText
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.border
            )
        )
    }
}