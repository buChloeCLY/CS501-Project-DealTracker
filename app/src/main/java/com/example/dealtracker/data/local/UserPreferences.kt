package com.example.dealtracker.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.dealtracker.domain.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists user information and settings using SharedPreferences.
 * Provides real-time observable state via StateFlow.
 */
object UserPreferences {

    private const val PREF_NAME = "DealTrackerPrefs"
    private const val KEY_USER = "user"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_FONT_SCALE = "font_scale"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    // Flow for observing dark mode state
    private val _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: StateFlow<Boolean> get() = _darkModeFlow

    // Flow for observing font scale state
    private val _fontScaleFlow = MutableStateFlow(1f)
    val fontScaleFlow: StateFlow<Float> get() = _fontScaleFlow

    /**
     * Initializes SharedPreferences. Must be called once before use.
     * @param context Application context.
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            // Initialize Flow values from SharedPreferences
            _darkModeFlow.value = prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
            _fontScaleFlow.value = prefs?.getFloat(KEY_FONT_SCALE, 1f) ?: 1f
        }
    }

    /**
     * Ensures UserPreferences has been initialized.
     */
    private fun checkInit() {
        if (prefs == null) {
            throw IllegalStateException("UserPreferences not initialized. Call init(context) first.")
        }
    }

    /**
     * Saves the user object and sets the logged-in status to true.
     * @param user The user object to save.
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
     * Retrieves the saved user object.
     * @return The User object or null if not found or if parsing fails.
     */
    fun getUser(): User? {
        checkInit()
        val json = prefs?.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a user is currently logged in.
     * @return True if logged in, false otherwise.
     */
    fun isLoggedIn(): Boolean {
        checkInit()
        return prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    /**
     * Clears the saved user information and sets the logged-in status to false.
     */
    fun clearUser() {
        checkInit()
        prefs?.edit()
            ?.remove(KEY_USER)
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.apply()
    }

    /**
     * Updates the saved user information.
     * @param user The updated user object.
     */
    fun updateUser(user: User) = saveUser(user)

    /**
     * Sets the dark mode preference. Updates both the Flow and SharedPreferences.
     * @param enabled True to enable dark mode, false to disable.
     */
    fun setDarkMode(enabled: Boolean) {
        checkInit()

        // Update Flow (for observing UI components)
        _darkModeFlow.value = enabled

        // Save to SP (for persistence)
        prefs?.edit()
            ?.putBoolean(KEY_DARK_MODE, enabled)
            ?.apply()
    }

    /**
     * Gets the current dark mode preference from SharedPreferences.
     * @return True if dark mode is enabled, false otherwise.
     */
    fun isDarkMode(): Boolean {
        checkInit()
        return prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
    }

    /**
     * Sets the font scale preference. Updates both the Flow and SharedPreferences.
     * @param scale The font scale value.
     */
    fun setFontScale(scale: Float) {
        checkInit()

        // Update Flow
        _fontScaleFlow.value = scale

        // Save to SP
        prefs?.edit()
            ?.putFloat(KEY_FONT_SCALE, scale)
            ?.apply()
    }

    /**
     * Gets the current font scale preference from SharedPreferences.
     * @return The font scale value.
     */
    fun getFontScale(): Float {
        checkInit()
        return prefs?.getFloat(KEY_FONT_SCALE, 1f) ?: 1f
    }
}