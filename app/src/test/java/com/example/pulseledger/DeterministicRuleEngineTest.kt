package com.example.pulseledger

import com.example.pulseledger.data.local.tier2.DeterministicRuleEngine
import com.example.pulseledger.ui.model.PulseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicRuleEngineTest {

    @Test
    fun testTransitAndTravelMerchants() {
        val merchants = listOf(
            "DMRC DELHI" to "Delhi Metro (DMRC)",
            "DELHI METRO RECHARGE" to "Delhi Metro",
            "IRCTC TICKETING" to "IRCTC",
            "RAILONE APPS" to "RailOne (Indian Railways)",
            "UBER INDIA SYSTEMS" to "Uber",
            "OLA RIDES" to "Ola Cabs",
            "RAPIDO BIKE" to "Rapido",
            "NMMT BUS PASS" to "NMMT Transit",
            "BMTC BANGALORE" to "BMTC Bus",
            "INDIGO AIRLINES" to "IndiGo Airlines",
            "HPCL PETROL PUMP" to "HPCL Fuel",
            "BPCL AUTO" to "BPCL Fuel",
            "IOCL OUTLET" to "Indian Oil (IOCL)"
        )

        for ((raw, expectedClean) in merchants) {
            val match = DeterministicRuleEngine.match(raw)
            assertNotNull("Failed matching: $raw", match)
            assertEquals("Category mismatch for $raw", PulseCategory.TRANSIT_AND_TRAVEL, match!!.category)
            assertEquals("Clean name mismatch for $raw", expectedClean, match.cleanMerchantName)
        }
    }

    @Test
    fun testGroceriesMerchants() {
        val merchants = listOf(
            "ZEPTO QUICK" to PulseCategory.GROCERIES,
            "BLINKIT COMMERCE" to PulseCategory.GROCERIES,
            "SWIGGY INSTAMART" to PulseCategory.GROCERIES,
            "BIGBASKET RETAIL" to PulseCategory.GROCERIES,
            "DMART HYPERMARKET" to PulseCategory.GROCERIES,
            "FRESH SIGNATURE" to PulseCategory.GROCERIES,
            "MOTHER DAIRY BOOTH" to PulseCategory.GROCERIES
        )

        for ((raw, expectedCat) in merchants) {
            val match = DeterministicRuleEngine.match(raw)
            assertNotNull("Failed matching: $raw", match)
            assertEquals("Category mismatch for $raw", expectedCat, match!!.category)
        }
    }

    @Test
    fun testFoodAndDiningMerchants() {
        val merchants = listOf(
            "MCDONALDS CONNAUGHT PLACE" to "McDonald's",
            "SWIGGY GOURMET" to "Swiggy",
            "ZOMATO RESTAURANT" to "Zomato",
            "DOMINOS PIZZA" to "Domino's Pizza",
            "KFC CHICKEN" to "KFC",
            "STARBUCKS COFFEE" to "Starbucks",
            "BURGER KING OUTLET" to "Burger King",
            "HALDIRAM SWEETS" to "Haldiram's"
        )

        for ((raw, expectedClean) in merchants) {
            val match = DeterministicRuleEngine.match(raw)
            assertNotNull("Failed matching: $raw", match)
            assertEquals(PulseCategory.FOOD_AND_DINING, match!!.category)
            assertEquals(expectedClean, match.cleanMerchantName)
        }
    }

    @Test
    fun testShoppingMerchants() {
        val merchants = listOf(
            "AMAZON PAYMENTS" to "Amazon",
            "FLIPKART INTERNET" to "Flipkart",
            "MYNTRA DESIGNS" to "Myntra",
            "AJIO TRENDS" to "Ajio",
            "MEESHO SUPPLY" to "Meesho",
            "NYKAA E-RETAIL" to "Nykaa",
            "DECATHLON SPORTS" to "Decathlon",
            "RANGOLI APPARELS" to "Rangoli"
        )

        for ((raw, expectedClean) in merchants) {
            val match = DeterministicRuleEngine.match(raw)
            assertNotNull("Failed matching: $raw", match)
            assertEquals(PulseCategory.SHOPPING, match!!.category)
            assertEquals(expectedClean, match.cleanMerchantName)
        }
    }

    @Test
    fun testBillsAndUtilitiesMerchants() {
        val merchants = listOf(
            "JIO RECHARGE" to PulseCategory.BILLS_AND_UTILITIES,
            "AIRTEL PREPAID" to PulseCategory.BILLS_AND_UTILITIES,
            "VI PREPAID BILL" to PulseCategory.BILLS_AND_UTILITIES,
            "BSES RAJDHANI" to PulseCategory.BILLS_AND_UTILITIES,
            "TATA POWER DDL" to PulseCategory.BILLS_AND_UTILITIES,
            "HOSTINGER INDIA" to PulseCategory.BILLS_AND_UTILITIES,
            "OPENAI SUBSCRIPTION" to PulseCategory.BILLS_AND_UTILITIES,
            "GOOGLE PLAY STORE" to PulseCategory.BILLS_AND_UTILITIES
        )

        for ((raw, expectedCat) in merchants) {
            val match = DeterministicRuleEngine.match(raw)
            assertNotNull("Failed matching: $raw", match)
            assertEquals(expectedCat, match!!.category)
        }
    }

    @Test
    fun testUpiVpaMatching() {
        val match = DeterministicRuleEngine.match(rawMerchant = "UPI Payment", upiVpa = "swiggy@icici")
        assertNotNull(match)
        assertEquals(PulseCategory.FOOD_AND_DINING, match!!.category)
        assertEquals("Swiggy", match.cleanMerchantName)
    }

    @Test
    fun testP2PZeroGuessingRule() {
        // True human personal names without business keywords
        assertTrue(DeterministicRuleEngine.isProbablePersonalName("Suresh Yadav"))
        assertTrue(DeterministicRuleEngine.isProbablePersonalName("Ramesh Kumar Sharma"))
        assertTrue(DeterministicRuleEngine.isProbablePersonalName("Priya Patel"))

        // Commercial entities with business keywords must not be treated as personal names
        assertFalse(DeterministicRuleEngine.isProbablePersonalName("Apollo Pharmacy Pvt Ltd"))
        assertFalse(DeterministicRuleEngine.isProbablePersonalName("Blinkit Store"))
        assertFalse(DeterministicRuleEngine.isProbablePersonalName("Sharma Chai Cafe"))
        assertFalse(DeterministicRuleEngine.isProbablePersonalName("Gupta Traders"))
    }
}
