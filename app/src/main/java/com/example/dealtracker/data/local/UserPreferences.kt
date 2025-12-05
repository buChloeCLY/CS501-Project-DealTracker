package com.example.dealtracker.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.dealtracker.domain.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 用户信息持久化 + 设置项持久化
 * 使用 SharedPreferences 保存，同时使用 StateFlow 提供实时可观察状态
 */
object UserPreferences {

    private const val PREF_NAME = "DealTrackerPrefs"
    private const val KEY_USER = "user"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_FONT_SCALE = "font_scale"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    // ========== 新增：用于 Compose 自动监听的 Flow ==========
    private val _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: StateFlow<Boolean> get() = _darkModeFlow

    private val _fontScaleFlow = MutableStateFlow(1f)
    val fontScaleFlow: StateFlow<Float> get() = _fontScaleFlow

    /**
     * 初始化 SharedPreferences
     * 在 MainActivity 中调用一次即可
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            // 初始化 Flow 的值（首次加载时同步 SP）
            _darkModeFlow.value = prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
            _fontScaleFlow.value = prefs?.getFloat(KEY_FONT_SCALE, 1f) ?: 1f
        }
    }

    /**
     * 确保已初始化
     */
    private fun checkInit() {
        if (prefs == null) {
            throw IllegalStateException("UserPreferences not initialized. Call init(context) first.")
        }
    }

    // ========== 用户信息 ==========
    fun saveUser(user: User) {
        checkInit()
        val userJson = gson.toJson(user)
        prefs?.edit()
            ?.putString(KEY_USER, userJson)
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.apply()
    }

    fun getUser(): User? {
        checkInit()
        val json = prefs?.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        checkInit()
        return prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    fun clearUser() {
        checkInit()
        prefs?.edit()
            ?.remove(KEY_USER)
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.apply()
    }

    fun updateUser(user: User) = saveUser(user)

    // ========== 深色模式（Flow + SP） ==========

    fun setDarkMode(enabled: Boolean) {
        checkInit()

        // 更新 Flow（Compose 自动重组）
        _darkModeFlow.value = enabled

        // 保存到 SP（持久化）
        prefs?.edit()
            ?.putBoolean(KEY_DARK_MODE, enabled)
            ?.apply()
    }

    fun isDarkMode(): Boolean {
        checkInit()
        return prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
    }

    // ========== 字体缩放（Flow + SP） ==========
    fun setFontScale(scale: Float) {
        checkInit()

        // 更新 Flow
        _fontScaleFlow.value = scale

        // 持久化
        prefs?.edit()
            ?.putFloat(KEY_FONT_SCALE, scale)
            ?.apply()
    }

    fun getFontScale(): Float {
        checkInit()
        return prefs?.getFloat(KEY_FONT_SCALE, 1f) ?: 1f
    }
}