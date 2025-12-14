package com.example.dealtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Light Mode Colors
object LightColorScheme {
    val primary = Color(0xFFFF6B35) // most of the text
    val onPrimary = Color.White
    val primaryContainer = Color(0xFFF3EDF7)
    val onPrimaryContainer = Color.Black // like the texts on category cards

    val secondary = Color(0xFF4CAF50)
    val onSecondary = Color.White
    val secondaryContainer = Color(0xFFE8F5E9)
    val onSecondaryContainer = Color(0xFF1B5E20)

    val backgroundLight = Color.White
    val onBackgroundLight = Color(0xFF1C1B1F)

    val surfaceLight = Color.White
    val onSurfaceLight = Color(0xFF1C1B1F)
    val surfaceVariantLight = Color(0xFFF5F5F5)
    val onSurfaceVariantLight = Color(0xFF49454F)

    val error = Color(0xFFE53935)
    val onError = Color.White

    val outline = Color(0xFFE0E0E0)
    val outlineVariant = Color(0xFFF0F0F0)
}

// Dark Mode Colors
object DarkColorScheme {
    val primary = Color(0xFFFF8A65)
    val onPrimary = Color(0xFF2C1810)
    val primaryContainer = Color(0xFF1F1D23)
    val onPrimaryContainer = Color.White
    val secondary = Color(0xFF81C784)
    val onSecondary = Color(0xFF1B5E20)
    val secondaryContainer = Color(0xFF2E7D32)
    val onSecondaryContainer = Color(0xFFC8E6C9)

    val backgroundDark = Color(0xFF1C1B1F)
    val onBackgroundDark = Color(0xFFE6E1E5)

    val surfaceDark = Color(0xFF1C1B1F)
    val onSurfaceDark = Color(0xFFE6E1E5)
    val surfaceVariantDark = Color(0xFF2B2930)
    val onSurfaceVariantDark = Color(0xFFCAC4D0)

    val error = Color(0xFFEF5350)
    val onError = Color(0xFF601410)

    val outline = Color(0xFF3E3E3E)
    val outlineVariant = Color(0xFF2B2B2B)
}

class AppColorScheme(
    val isDark: Boolean
) {
    private val light = LightColorScheme
    private val dark = DarkColorScheme

    val primaryText = if (isDark) dark.onBackgroundDark else light.onBackgroundLight
    val secondaryText = if (isDark) dark.onSurfaceVariantDark else light.onSurfaceVariantLight
    val tertiaryText = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)

    val background = if (isDark) dark.backgroundDark else light.backgroundLight
    val surface = if (isDark) dark.surfaceDark else light.surfaceLight
    val card = if (isDark) dark.surfaceVariantDark else light.surfaceVariantLight

    val accent = if (isDark) dark.primary else light.primary
    val accentSecondary = if (isDark) dark.secondary else light.secondary
    val onPrimary = if (isDark) dark.onPrimary else light.onPrimary

    val success = if (isDark) dark.secondary else light.secondary
    val warning = Color(0xFFFFA726)
    val error = if (isDark) dark.error else light.error
    val info = Color(0xFF42A5F5)

    val border = if (isDark) dark.outline else light.outline
    val divider = if (isDark) dark.outlineVariant else light.outlineVariant

    val priceColor = if (isDark) dark.primary else light.primary
    val discountColor = if (isDark) dark.secondary else light.secondary
    val ratingColor = Color(0xFFFFC107)

    val topBarBackground = if (isDark) dark.primaryContainer else light.primaryContainer
    val topBarContent = if (isDark) dark.onPrimaryContainer else light.onPrimaryContainer

    val bottomBarBackground = if (isDark) dark.primaryContainer else light.primaryContainer
    val bottomBarSelected = if (isDark) dark.primary else light.primary
    val bottomBarUnselected = if (isDark) Color.White else Color.Black
}