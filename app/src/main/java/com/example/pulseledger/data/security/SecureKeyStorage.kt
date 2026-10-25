package com.example.pulseledger.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.pulseledger.BuildConfig

/**
 * Key storage for Gemini API key.
 *
 * Precedence order:
 * 1. User-configured custom API key saved in SharedPreferences.
 * 2. Build-time injected API key from BuildConfig.GEMINI_API_KEY (via local.properties).
 * 3. Null (app operates 100% on-device with Regex & Merchant Memory).
 */
open class SecureKeyStorage(context: Context) : KeyStorageProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getGeminiApiKey(): String? {
        val userCustomKey = prefs.getString(KEY_USER_GEMINI_KEY, null)?.trim()
        if (!userCustomKey.isNullOrBlank()) {
            return userCustomKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        return buildKey.ifBlank { null }
    }

    override fun setGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_USER_GEMINI_KEY, apiKey.trim()).apply()
    }

    override fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_USER_GEMINI_KEY).apply()
    }

    companion object {
        private const val PREFS_NAME = "spendlog_secure_keys"
        private const val KEY_USER_GEMINI_KEY = "key_user_gemini_api"
    }
}
