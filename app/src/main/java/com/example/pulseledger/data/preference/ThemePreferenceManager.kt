package com.example.pulseledger.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported Visual Theme Modes.
 */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    val displayName: String
        get() = when (this) {
            DARK -> "Dark"
            LIGHT -> "Light"
            SYSTEM -> "System"
        }
}

/**
 * Manages persisting and live-streaming user theme preferences across the application.
 */
class ThemePreferenceManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.DARK.name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "pulse_ledger_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        @Volatile
        private var instance: ThemePreferenceManager? = null

        fun getInstance(context: Context): ThemePreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferenceManager(context).also { instance = it }
            }
        }
    }
}
