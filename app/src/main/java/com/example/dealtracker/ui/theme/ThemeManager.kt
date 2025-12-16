package com.example.dealtracker.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object ThemeManager {
    private val _isDarkMode = MutableStateFlow(false)

    fun isDarkMode(context: Context): Flow<Boolean> {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean("dark_mode", false)
        return _isDarkMode
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", isDark).apply()
        _isDarkMode.value = isDark
    }
}