package com.example.pulseledger.service

import com.example.pulseledger.ui.model.PulseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ActionableNotificationHelperTest {

    @Test
    fun testUnder100RupeesReturnsMicroCategoryActions() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9) // Morning 9 AM
            set(Calendar.MINUTE, 30)
        }
        val timestamp = calendar.timeInMillis

        val actions = ActionableNotificationHelper.getContextualActions(amount = 40.0, timestamp = timestamp)

        assertEquals(3, actions.size)
        // Must contain Food/Tea, Transit, Grocery
        assertTrue(actions.any { it.category == PulseCategory.FOOD_AND_DINING && it.label.contains("Food/Tea") })
        assertTrue(actions.any { it.category == PulseCategory.TRANSIT_AND_TRAVEL && it.label.contains("Transit") })
        assertTrue(actions.any { it.category == PulseCategory.GROCERIES && it.label.contains("Grocery") })
    }

    @Test
    fun testOver100RupeesDaytimeReturnsCommercialCategoryActions() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14) // Afternoon 2 PM
            set(Calendar.MINUTE, 0)
        }
        val timestamp = calendar.timeInMillis

        val actions = ActionableNotificationHelper.getContextualActions(amount = 450.0, timestamp = timestamp)

        assertEquals(3, actions.size)
        // Must contain Dining, Shopping, Bills
        assertTrue(actions.any { it.category == PulseCategory.FOOD_AND_DINING && it.label.contains("Dining") })
        assertTrue(actions.any { it.category == PulseCategory.SHOPPING && it.label.contains("Shopping") })
        assertTrue(actions.any { it.category == PulseCategory.BILLS_AND_UTILITIES && it.label.contains("Bills") })
    }

    @Test
    fun testOver100RupeesLateNightReturnsDiningAndEntertainment() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22) // Night 10 PM
            set(Calendar.MINUTE, 15)
        }
        val timestamp = calendar.timeInMillis

        val actions = ActionableNotificationHelper.getContextualActions(amount = 1200.0, timestamp = timestamp)

        assertEquals(3, actions.size)
        assertTrue(actions.any { it.category == PulseCategory.FOOD_AND_DINING && it.label.contains("Dining") })
        assertTrue(actions.any { it.category == PulseCategory.ENTERTAINMENT && it.label.contains("Entertainment") })
        assertTrue(actions.any { it.category == PulseCategory.TRANSIT_AND_TRAVEL || it.category == PulseCategory.SHOPPING })
    }
}
