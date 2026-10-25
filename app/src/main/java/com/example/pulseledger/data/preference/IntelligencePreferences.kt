package com.example.pulseledger.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Step 8.5 Intelligence Preferences:
 * - Monthly Variable Budget (default ₹10,000) for Safe-to-Spend Today.
 * - Active Trip / Event Mode Tag (e.g., "#UjjainTrip", "#Hackathon").
 */
class IntelligencePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _monthlyBudget = MutableStateFlow(
        prefs.getFloat(KEY_MONTHLY_BUDGET, DEFAULT_MONTHLY_BUDGET).toDouble()
    )
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _activeEventTag = MutableStateFlow(
        prefs.getString(KEY_ACTIVE_EVENT_TAG, null)
    )
    val activeEventTag: StateFlow<String?> = _activeEventTag.asStateFlow()

    fun setMonthlyBudget(budget: Double) {
        val safeBudget = if (budget <= 0.0) DEFAULT_MONTHLY_BUDGET.toDouble() else budget
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET, safeBudget.toFloat()).apply()
        _monthlyBudget.value = safeBudget
    }

    fun setActiveEventTag(tag: String?) {
        val cleanTag = tag?.trim()?.ifBlank { null }
        val formattedTag = cleanTag?.let {
            if (it.startsWith("#")) it else "#$it"
        }
        prefs.edit().putString(KEY_ACTIVE_EVENT_TAG, formattedTag).apply()
        _activeEventTag.value = formattedTag
    }

    fun clearActiveEventTag() {
        setActiveEventTag(null)
    }

    companion object {
        private const val PREFS_NAME = "pulse_intelligence_prefs"
        private const val KEY_MONTHLY_BUDGET = "monthly_variable_budget"
        private const val KEY_ACTIVE_EVENT_TAG = "active_event_tag"
        const val DEFAULT_MONTHLY_BUDGET = 10000.0f

        @Volatile
        private var INSTANCE: IntelligencePreferences? = null

        fun getInstance(context: Context): IntelligencePreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = IntelligencePreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
