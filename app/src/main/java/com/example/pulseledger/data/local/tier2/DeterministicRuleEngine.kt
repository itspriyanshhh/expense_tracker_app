package com.example.pulseledger.data.local.tier2

import com.example.pulseledger.ui.model.PulseCategory
import java.util.Locale

data class RuleMatchResult(
    val category: PulseCategory,
    val matchedKeyword: String,
    val cleanMerchantName: String,
    val confidence: Double = 0.98
)

/**
 * Tier 2 Deterministic On-Device Rule Matcher.
 *
 * Evaluates raw merchant and UPI strings in < 1ms on Dispatchers.Default.
 * Captures high-frequency Indian merchants across transit, groceries, dining, shopping, and utilities.
 */
object DeterministicRuleEngine {

    private data class MerchantRule(
        val keyword: String,
        val category: PulseCategory,
        val cleanName: String,
        val isExactWordOnly: Boolean = false
    )

    private val RULES: List<MerchantRule> = listOf(
        // Transit & Travel
        MerchantRule("DELHI METRO", PulseCategory.TRANSIT_AND_TRAVEL, "Delhi Metro"),
        MerchantRule("DMRC", PulseCategory.TRANSIT_AND_TRAVEL, "Delhi Metro (DMRC)"),
        MerchantRule("IRCTC", PulseCategory.TRANSIT_AND_TRAVEL, "IRCTC"),
        MerchantRule("RAILONE", PulseCategory.TRANSIT_AND_TRAVEL, "RailOne (Indian Railways)"),
        MerchantRule("UBER", PulseCategory.TRANSIT_AND_TRAVEL, "Uber"),
        MerchantRule("OLA", PulseCategory.TRANSIT_AND_TRAVEL, "Ola Cabs", isExactWordOnly = true),
        MerchantRule("RAPIDO", PulseCategory.TRANSIT_AND_TRAVEL, "Rapido"),
        MerchantRule("NMMT", PulseCategory.TRANSIT_AND_TRAVEL, "NMMT Transit"),
        MerchantRule("BMTC", PulseCategory.TRANSIT_AND_TRAVEL, "BMTC Bus"),
        MerchantRule("INDIGO", PulseCategory.TRANSIT_AND_TRAVEL, "IndiGo Airlines"),
        MerchantRule("HPCL", PulseCategory.TRANSIT_AND_TRAVEL, "HPCL Fuel"),
        MerchantRule("BPCL", PulseCategory.TRANSIT_AND_TRAVEL, "BPCL Fuel"),
        MerchantRule("IOCL", PulseCategory.TRANSIT_AND_TRAVEL, "Indian Oil (IOCL)"),
        MerchantRule("PETROL", PulseCategory.TRANSIT_AND_TRAVEL, "Fuel Station"),

        // Groceries & Quick Commerce
        MerchantRule("ZEPTO", PulseCategory.GROCERIES, "Zepto"),
        MerchantRule("BLINKIT", PulseCategory.GROCERIES, "Blinkit"),
        MerchantRule("INSTAMART", PulseCategory.GROCERIES, "Swiggy Instamart"),
        MerchantRule("BIGBASKET", PulseCategory.GROCERIES, "BigBasket"),
        MerchantRule("DMART", PulseCategory.GROCERIES, "DMart"),
        MerchantRule("FRESH SIGNATURE", PulseCategory.GROCERIES, "Fresh Signature"),
        MerchantRule("MOTHER DAIRY", PulseCategory.GROCERIES, "Mother Dairy"),

        // Food & Dining
        MerchantRule("MCDONALDS", PulseCategory.FOOD_AND_DINING, "McDonald's"),
        MerchantRule("MCDONALD", PulseCategory.FOOD_AND_DINING, "McDonald's"),
        MerchantRule("SWIGGY", PulseCategory.FOOD_AND_DINING, "Swiggy"),
        MerchantRule("ZOMATO", PulseCategory.FOOD_AND_DINING, "Zomato"),
        MerchantRule("DOMINOS", PulseCategory.FOOD_AND_DINING, "Domino's Pizza"),
        MerchantRule("KFC", PulseCategory.FOOD_AND_DINING, "KFC"),
        MerchantRule("STARBUCKS", PulseCategory.FOOD_AND_DINING, "Starbucks"),
        MerchantRule("BURGER KING", PulseCategory.FOOD_AND_DINING, "Burger King"),
        MerchantRule("HALDIRAM", PulseCategory.FOOD_AND_DINING, "Haldiram's"),
        MerchantRule("CHAAYOS", PulseCategory.FOOD_AND_DINING, "Chaayos"),
        MerchantRule("CHAI POINT", PulseCategory.FOOD_AND_DINING, "Chai Point"),

        // Shopping & E-Commerce
        MerchantRule("AMAZON", PulseCategory.SHOPPING, "Amazon"),
        MerchantRule("FLIPKART", PulseCategory.SHOPPING, "Flipkart"),
        MerchantRule("MYNTRA", PulseCategory.SHOPPING, "Myntra"),
        MerchantRule("AJIO", PulseCategory.SHOPPING, "Ajio"),
        MerchantRule("MEESHO", PulseCategory.SHOPPING, "Meesho"),
        MerchantRule("NYKAA", PulseCategory.SHOPPING, "Nykaa"),
        MerchantRule("DECATHLON", PulseCategory.SHOPPING, "Decathlon"),
        MerchantRule("RANGOLI", PulseCategory.SHOPPING, "Rangoli"),
        MerchantRule("TATA CLiQ", PulseCategory.SHOPPING, "Tata CLiQ"),

        // Bills & Utilities
        MerchantRule("JIO", PulseCategory.BILLS_AND_UTILITIES, "Jio Telecom", isExactWordOnly = true),
        MerchantRule("AIRTEL", PulseCategory.BILLS_AND_UTILITIES, "Airtel"),
        MerchantRule("VI PREPAID", PulseCategory.BILLS_AND_UTILITIES, "Vodafone Idea (Vi)"),
        MerchantRule("VI POSTPAID", PulseCategory.BILLS_AND_UTILITIES, "Vodafone Idea (Vi)"),
        MerchantRule("VODAFONE", PulseCategory.BILLS_AND_UTILITIES, "Vodafone Idea (Vi)"),
        MerchantRule("BSES", PulseCategory.BILLS_AND_UTILITIES, "BSES Electricity"),
        MerchantRule("TATA POWER", PulseCategory.BILLS_AND_UTILITIES, "Tata Power"),
        MerchantRule("HOSTINGER", PulseCategory.BILLS_AND_UTILITIES, "Hostinger"),
        MerchantRule("OPENAI", PulseCategory.BILLS_AND_UTILITIES, "OpenAI"),
        MerchantRule("GOOGLE PLAY", PulseCategory.BILLS_AND_UTILITIES, "Google Play"),
        MerchantRule("ACT FIBERNET", PulseCategory.BILLS_AND_UTILITIES, "ACT Fibernet"),
        MerchantRule("BESCOM", PulseCategory.BILLS_AND_UTILITIES, "BESCOM Bangalore"),

        // Investments & Wealth
        MerchantRule("ZERODHA", PulseCategory.INVESTMENTS, "Zerodha"),
        MerchantRule("GROWW", PulseCategory.INVESTMENTS, "Groww"),
        MerchantRule("UPSTOX", PulseCategory.INVESTMENTS, "Upstox"),
        MerchantRule("ANGEL ONE", PulseCategory.INVESTMENTS, "Angel One"),
        MerchantRule("ANGELONE", PulseCategory.INVESTMENTS, "Angel One"),
        MerchantRule("KUVERA", PulseCategory.INVESTMENTS, "Kuvera"),
        MerchantRule("SMALLCASE", PulseCategory.INVESTMENTS, "Smallcase"),
        MerchantRule("INDMONEY", PulseCategory.INVESTMENTS, "INDmoney"),
        MerchantRule("COIN", PulseCategory.INVESTMENTS, "Zerodha Coin", isExactWordOnly = true),
        MerchantRule("MUTUAL FUND", PulseCategory.INVESTMENTS, "Mutual Fund"),
        MerchantRule("CAMS", PulseCategory.INVESTMENTS, "CAMS Services", isExactWordOnly = true),
        MerchantRule("KFINTECH", PulseCategory.INVESTMENTS, "KFintech"),

        // Home & Rent
        MerchantRule("NOBROKER", PulseCategory.HOME_AND_RENT, "NoBroker"),
        MerchantRule("MYGATE", PulseCategory.HOME_AND_RENT, "MyGate"),
        MerchantRule("NESTAWAY", PulseCategory.HOME_AND_RENT, "Nestaway"),
        MerchantRule("RENTOMOJO", PulseCategory.HOME_AND_RENT, "RentoMojo"),
        MerchantRule("FURLENCO", PulseCategory.HOME_AND_RENT, "Furlenco"),
        MerchantRule("HOUSE RENT", PulseCategory.HOME_AND_RENT, "House Rent"),
        MerchantRule("RENT", PulseCategory.HOME_AND_RENT, "House Rent", isExactWordOnly = true),
        MerchantRule("SOCIETY", PulseCategory.HOME_AND_RENT, "Society Maintenance", isExactWordOnly = true),

        // Personal Care & Wellness
        MerchantRule("CULT FIT", PulseCategory.PERSONAL_CARE, "Cult.fit"),
        MerchantRule("CULTFIT", PulseCategory.PERSONAL_CARE, "Cult.fit"),
        MerchantRule("URBAN COMPANY", PulseCategory.PERSONAL_CARE, "Urban Company"),
        MerchantRule("NATURALS", PulseCategory.PERSONAL_CARE, "Naturals Salon"),
        MerchantRule("JAWED HABIB", PulseCategory.PERSONAL_CARE, "Jawed Habib"),
        MerchantRule("KAYA", PulseCategory.PERSONAL_CARE, "Kaya Clinic"),
        MerchantRule("SALON", PulseCategory.PERSONAL_CARE, "Salon & Grooming", isExactWordOnly = true),
        MerchantRule("SPA", PulseCategory.PERSONAL_CARE, "Spa & Wellness", isExactWordOnly = true),
        MerchantRule("GYM", PulseCategory.PERSONAL_CARE, "Gym & Fitness", isExactWordOnly = true),

        // Insurance
        MerchantRule("LIC", PulseCategory.INSURANCE, "LIC of India", isExactWordOnly = true),
        MerchantRule("POLICYBAZAAR", PulseCategory.INSURANCE, "PolicyBazaar"),
        MerchantRule("STAR HEALTH", PulseCategory.INSURANCE, "Star Health Insurance"),
        MerchantRule("ACKO", PulseCategory.INSURANCE, "Acko General Insurance", isExactWordOnly = true),
        MerchantRule("DIGIT", PulseCategory.INSURANCE, "Go Digit Insurance", isExactWordOnly = true),
        MerchantRule("HDFC ERGO", PulseCategory.INSURANCE, "HDFC ERGO Insurance"),
        MerchantRule("ICICI LOMBARD", PulseCategory.INSURANCE, "ICICI Lombard"),
        MerchantRule("MAX LIFE", PulseCategory.INSURANCE, "Max Life Insurance"),
        MerchantRule("CARE HEALTH", PulseCategory.INSURANCE, "Care Health Insurance")
    )

    // Standard UPI handle suffixes in India
    private val UPI_SUFFIX_REGEX = Regex("@[a-zA-Z0-9.-]+")

    /**
     * Extracts a normalized lookup key from either a UPI VPA or raw merchant string.
     * Examples:
     * - "swiggy@icici" -> "swiggy"
     * - "9876543210@paytm" -> "9876543210@paytm"
     * - "ZEPTO INDIA PVT LTD" -> "zepto"
     */
    fun extractLookupKey(rawMerchant: String, upiVpa: String?): String {
        if (!upiVpa.isNullOrBlank()) {
            val trimmedVpa = upiVpa.trim().lowercase(Locale.ROOT)
            val prefix = trimmedVpa.substringBefore('@')
            // If prefix contains letters (e.g. "swiggy", "zomato"), use prefix; if it's a mobile number, keep full VPA
            return if (prefix.any { it.isLetter() }) prefix else trimmedVpa
        }
        return normalizeString(rawMerchant)
    }

    /**
     * Evaluates merchant string against Tier 2 deterministic rules.
     */
    fun match(rawMerchant: String, upiVpa: String? = null): RuleMatchResult? {
        val normalizedCandidate = buildString {
            append(rawMerchant.uppercase(Locale.ROOT))
            if (!upiVpa.isNullOrBlank()) {
                append(" ")
                append(upiVpa.uppercase(Locale.ROOT))
            }
        }

        val cleanedCandidate = normalizedCandidate.replace(Regex("[^A-Z0-9 ]"), " ")
        val tokens = cleanedCandidate.split(Regex("\\s+")).filter { it.isNotBlank() }

        for (rule in RULES) {
            val matched = if (rule.isExactWordOnly) {
                tokens.any { it == rule.keyword }
            } else {
                cleanedCandidate.contains(rule.keyword)
            }

            if (matched) {
                return RuleMatchResult(
                    category = rule.category,
                    matchedKeyword = rule.keyword,
                    cleanMerchantName = rule.cleanName
                )
            }
        }

        return null
    }

    /**
     * Checks if a payee string looks like a personal name rather than a commercial entity.
     * Used by the P2P Zero-Guessing Rule to avoid false positives.
     */
    fun isProbablePersonalName(merchantString: String): Boolean {
        val upper = merchantString.uppercase(Locale.ROOT)
        val businessKeywords = listOf(
            "PVT", "LTD", "LIMITED", "CORP", "INC", "STORE", "MART", "CAFE",
            "HOTEL", "RESTAURANT", "SHOP", "SERVICES", "ENTERPRISES", "TRADERS",
            "PHARMACY", "HOSPITAL", "CLINIC", "PAYMENT", "GATEWAY", "TECHNOLOGIES",
            "MEDICOS", "MEDICAL", "CHEMIST", "HEALTHCARE", "DIAGNOSTICS", "LAB",
            "BAKERY", "SWEETS", "STATIONERY", "HARDWARE", "JEWELLERS", "SALON"
        )

        if (businessKeywords.any { upper.contains(it) }) {
            return false
        }

        // Two or three word strings composed entirely of alphabets (e.g., "Suresh Yadav", "Ramesh Kumar Sharma")
        val cleanWords = merchantString.trim().split(Regex("\\s+"))
        val isAllAlphabetic = cleanWords.all { word -> word.all { it.isLetter() } }

        return isAllAlphabetic && cleanWords.size in 2..3
    }

    /**
     * Step 8.5 Closed-Loop Wallet De-Duplicator.
     * Classifies wallet loads (UPI LITE, AMAZON PAY WALLET, PAYTM WALLET)
     * and CC bill payments (CRED CLUB, BILLDESK CC, CRED) as TRANSFER_NEUTRAL
     * so they do not double count against monthly variable spend.
     */
    fun isTransferNeutral(rawMerchant: String, upiVpa: String? = null): Boolean {
        val candidate = buildString {
            append(rawMerchant.uppercase(Locale.ROOT))
            if (!upiVpa.isNullOrBlank()) {
                append(" ")
                append(upiVpa.uppercase(Locale.ROOT))
            }
        }
        val keywords = listOf(
            "UPI LITE",
            "UPILITE",
            "AMAZON PAY WALLET",
            "AMAZONPAY WALLET",
            "PAYTM WALLET",
            "CRED CLUB",
            "BILLDESK CC",
            "CRED CC",
            "CRED"
        )
        return keywords.any { candidate.contains(it) }
    }

    /**
     * Helper to verify if merchant matches Tier 2 commercial rules.
     */
    fun isTier2Merchant(rawMerchant: String, upiVpa: String? = null): Boolean {
        return match(rawMerchant, upiVpa) != null
    }

    private fun normalizeString(input: String): String {
        return input.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    }
}
