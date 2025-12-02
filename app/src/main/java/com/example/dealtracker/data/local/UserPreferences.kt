package com.example.dealtracker.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.dealtracker.domain.model.User
import com.google.gson.Gson

/**
 * 用户信息持久化存储
 * 使用 SharedPreferences 保存用户登录状态
 */
object UserPreferences {

    private const val PREF_NAME = "DealTrackerPrefs"
    private const val KEY_USER = "user"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    /**
     * 初始化 SharedPreferences
     * 在 MainActivity 中调用一次即可
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
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

    /**
     * 保存用户信息
     */
    fun saveUser(user: User) {
        checkInit()
        val userJson = gson.toJson(user)
        prefs?.edit()
            ?.putString(KEY_USER, userJson)
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.apply()
    }

    /**
     * 获取用户信息
     */
    fun getUser(): User? {
        checkInit()
        val userJson = prefs?.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        checkInit()
        return prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    /**
     * 清除用户信息（退出登录）
     */
    fun clearUser() {
        checkInit()
        prefs?.edit()
            ?.remove(KEY_USER)
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.apply()
    }

    /**
     * 更新用户信息
     */
    fun updateUser(user: User) {
        saveUser(user)
    }
}