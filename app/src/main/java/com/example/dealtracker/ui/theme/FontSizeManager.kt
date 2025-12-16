package com.example.dealtracker.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

enum class FontSize(val scale: Float, val displayName: String) {
    SMALL(0.85f, "Small"),
    MEDIUM(1.0f, "Medium"),
    LARGE(1.15f, "Large")
}

// AI helps to find the hard-coded fontsize and design this manager
object FontSizeManager {
    private val _currentFontSize = MutableStateFlow(FontSize.MEDIUM)
    val currentFontSize: Flow<FontSize> = _currentFontSize

    fun getFontSize(context: Context): FontSize {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val scale = prefs.getFloat("font_scale", 1.0f)
        _currentFontSize.value = when {
            scale <= 0.9f -> FontSize.SMALL
            scale >= 1.1f -> FontSize.LARGE
            else -> FontSize.MEDIUM
        }
        return _currentFontSize.value
    }

    fun setFontSize(context: Context, fontSize: FontSize) {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("font_scale", fontSize.scale).apply()
        _currentFontSize.value = fontSize
    }

    fun getCurrentScale(): Float {
        return _currentFontSize.value.scale
    }
}