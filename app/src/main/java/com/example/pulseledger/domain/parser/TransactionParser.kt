package com.example.pulseledger.domain.parser

import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.UpcomingMandateEntity
import com.example.pulseledger.data.local.tier2.DeterministicRuleEngine
import java.util.Calendar
import java.util.Locale

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val rawMerchant: String,
    val upiVpa: String? = null,
    val upiRefNo: String? = null,
    val bankOrAppSource: String,
    val timestamp: Long = System.currentTimeMillis(),
    val netAmount: Double = if (type == TransactionType.TRANSFER_NEUTRAL) 0.0 else amount,
    val parentTxId: String? = null,
    val isRefund: Boolean = false,
    val eventTag: String? = null
)

/**
 * High-performance deterministic SMS and notification text parser for the Indian fintech ecosystem.
 */
object TransactionParser {

    // Step A: Rejection Filter Patterns
    // Note: Upcoming mandates with "will be debited", "e-mandate", "auto-debit" are intercepted by parseMandate()
    private val REJECTION_PATTERNS = listOf(
        Regex("(?i)\\bOTP\\b"),
        Regex("(?i)verification code"),
        Regex("(?i)pre-approved"),
        Regex("(?i)available limit"),
        Regex("(?i)\\bfailed\\b"),
        Regex("(?i)\\bdeclined\\b"),
        Regex("(?i)requested money"),
        Regex("(?i)payment request"),
        Regex("(?i)minimum amount due")
    )

    // Step B: Action Verb Patterns
    private val DEBIT_PATTERNS = listOf(
        Regex("(?i)\\bdebited\\b"),
        Regex("(?i)\\bspent\\b"),
        Regex("(?i)\\bpaid\\b"),
        Regex("(?i)\\bsent\\b"),
        Regex("(?i)\\bwithdrawn\\b")
    )

    private val CREDIT_PATTERNS = listOf(
        Regex("(?i)\\bcredited\\b"),
        Regex("(?i)\\breceived\\b"),
        Regex("(?i)\\brefunded\\b"),
        Regex("(?i)\\brefund\\b"),
        Regex("(?i)\\badded\\b")
    )

    // Step C: Currency Amount Regex
    // Robust regex supporting: "Rs. 1,240.50", "INR 60", "₹450", "Rs 99.00", "Rs 1250.00", "₹ 200"
    private val AMOUNT_REGEX = Regex(
        "(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
    )
    private val AMOUNT_POSTFIX_REGEX = Regex(
        "([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹)",
        RegexOption.IGNORE_CASE
    )

    // UPI VPA Regex: e.g. "swiggy@icici", "9876543210@ybl", "name.123@okhdfcbank"
    private val UPI_VPA_REGEX = Regex("([a-zA-Z0-9.\\-_]+@[a-zA-Z]+)")

    // 12-digit UPI UTR / RRN Reference Number
    private val PREF_12_DIGIT_REGEX = Regex(
        "(?i)(?:upi\\s*(?:ref|reference)?\\s*(?:no\\.?)?|utr(?:\\s*no\\.?)?|ref(?:\\s*no\\.?)?|rrn(?:\\s*no\\.?)?)[:\\s]*([0-9]{12})\\b"
    )
    private val STANDALONE_12_DIGIT_REGEX = Regex("\\b([0-9]{12})\\b")

    /**
     * Step 8.5 Pre-Debit Interceptor:
     * Parses incoming text into an UpcomingMandateEntity if it announces an upcoming auto-debit,
     * e-mandate, or subscription renewal (e.g. OpenAI, Netflix, SIPs).
     */
    fun parseMandate(
        text: String,
        timestamp: Long = System.currentTimeMillis()
    ): UpcomingMandateEntity? {
        if (text.isBlank()) return null
        val normalizedText = text.replace("\n", " ").trim()

        val isMandateAlert = normalizedText.contains("will be debited", ignoreCase = true) ||
            normalizedText.contains("e-mandate", ignoreCase = true) ||
            normalizedText.contains("emandate", ignoreCase = true) ||
            normalizedText.contains("auto-debit", ignoreCase = true) ||
            normalizedText.contains("autodebit", ignoreCase = true) ||
            normalizedText.contains("autopay", ignoreCase = true) ||
            normalizedText.contains("mandate", ignoreCase = true) ||
            (normalizedText.contains("due", ignoreCase = true) && normalizedText.contains("debited", ignoreCase = true))

        if (!isMandateAlert) return null

        val amount = extractAmount(normalizedText) ?: return null
        val merchant = extractMandateMerchant(normalizedText)
        val dueDate = extractMandateDueDate(normalizedText, timestamp)

        val id = "mandate_${(amount * 100).toLong()}_${kotlin.math.abs(merchant.hashCode())}_${dueDate / 86400000}"

        return UpcomingMandateEntity(
            id = id,
            merchant = merchant,
            amount = amount,
            dueDate = dueDate,
            rawText = normalizedText,
            createdAt = timestamp
        )
    }

    /**
     * Parses an incoming notification title + body or SMS text.
     * Returns null if the text is promotional, an OTP, an upcoming mandate (handled separately), or non-transactional.
     */
    fun parse(
        text: String,
        senderOrPackage: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedTransaction? {
        if (text.isBlank()) return null

        val normalizedText = text.replace("\n", " ").trim()

        // 1. Step 8.5 Mandate Interceptor: do not treat upcoming scheduled debits as completed transactions
        if (parseMandate(normalizedText, timestamp) != null) {
            return null
        }

        // 2. Step A: Rejection Filter
        for (pattern in REJECTION_PATTERNS) {
            if (pattern.containsMatchIn(normalizedText)) {
                return null
            }
        }

        // 3. Step B: Action Verb Detection
        val isCredit = CREDIT_PATTERNS.any { it.containsMatchIn(normalizedText) }
        val isDebit = DEBIT_PATTERNS.any { it.containsMatchIn(normalizedText) }

        if (!isCredit && !isDebit) {
            return null
        }

        var transactionType = if (isCredit && !isDebit) {
            TransactionType.CREDIT
        } else if (isDebit && !isCredit) {
            TransactionType.DEBIT
        } else {
            // Both present: inspect keyword position or semantics
            if (normalizedText.contains("refund", ignoreCase = true) ||
                normalizedText.contains("credited", ignoreCase = true) ||
                normalizedText.contains("received", ignoreCase = true)
            ) {
                TransactionType.CREDIT
            } else {
                TransactionType.DEBIT
            }
        }

        // 4. Step C: Extract Amount
        val amount = extractAmount(normalizedText) ?: return null

        // 5. Extract UPI VPA
        val upiVpa = extractUpiVpa(normalizedText)

        // 6. Extract 12-digit Reference Number
        val upiRefNo = extractUpiRefNo(normalizedText)

        // 7. Extract Raw Merchant Name
        val rawMerchant = extractRawMerchant(normalizedText, transactionType, upiVpa)

        // 8. Step 8.5 Closed-Loop Wallet De-Duplicator:
        // Top-ups to UPI Lite, Paytm Wallet, Amazon Pay Wallet, or CC bill payments are classified as TRANSFER_NEUTRAL
        if (DeterministicRuleEngine.isTransferNeutral(rawMerchant, upiVpa)) {
            transactionType = TransactionType.TRANSFER_NEUTRAL
        }

        // 9. Resolve Source (Bank or UPI App)
        val bankOrAppSource = resolveSource(senderOrPackage, normalizedText)

        return ParsedTransaction(
            amount = amount,
            type = transactionType,
            rawMerchant = rawMerchant,
            upiVpa = upiVpa,
            upiRefNo = upiRefNo,
            bankOrAppSource = bankOrAppSource,
            timestamp = timestamp,
            netAmount = if (transactionType == TransactionType.TRANSFER_NEUTRAL) 0.0 else amount
        )
    }

    private fun extractMandateMerchant(text: String): String {
        val tier2 = DeterministicRuleEngine.match(text)
        if (tier2 != null) {
            return tier2.cleanMerchantName
        }

        val towardsRegex = Regex("(?i)(?:towards|for|at|to)\\s+([A-Za-z0-9\\s.&'\\-_]+?)(?:\\s+(?:auto-?debit|autopay|mandate|will|is|dated|on|in|ref|a\\/c)|\\.|$)")
        val match = towardsRegex.find(text)
        if (match != null) {
            val name = cleanMerchantString(match.groupValues[1])
            if (name.isNotBlank() && !name.equals("VPA", ignoreCase = true)) {
                return name
            }
        }

        return "AutoPay Subscription"
    }

    private fun extractMandateDueDate(text: String, baseTimestamp: Long): Long {
        val lower = text.lowercase(Locale.ROOT)
        when {
            lower.contains("in 24h") || lower.contains("in 24 hours") || lower.contains("in 24 hrs") || lower.contains("tomorrow") -> {
                return baseTimestamp + 24 * 3600 * 1000
            }
            lower.contains("in 48h") || lower.contains("in 48 hours") || lower.contains("in 48 hrs") -> {
                return baseTimestamp + 48 * 3600 * 1000
            }
            lower.contains("in 72h") || lower.contains("in 72 hours") || lower.contains("in 3 days") -> {
                return baseTimestamp + 72 * 3600 * 1000
            }
        }

        val dateRegex = Regex("(?:on|due on|dated|by)\\s+([0-9]{1,2}[-/.][A-Za-z0-9]{2,9}(?:[-/.][0-9]{2,4})?)", RegexOption.IGNORE_CASE)
        val dateMatch = dateRegex.find(text)
        if (dateMatch != null) {
            val dateStr = dateMatch.groupValues[1]
            val parsedDate = tryParseDate(dateStr, baseTimestamp)
            if (parsedDate != null) return parsedDate
        }

        return baseTimestamp + 24 * 3600 * 1000
    }

    private fun tryParseDate(dateStr: String, fallbackTimestamp: Long): Long? {
        val formats = listOf(
            "dd-MMM-yy",
            "dd-MMM-yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd-MMM"
        )
        for (pattern in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(dateStr)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    if (pattern == "dd-MMM") {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = fallbackTimestamp }
                        cal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun extractAmount(text: String): Double? {
        val match = AMOUNT_REGEX.find(text) ?: AMOUNT_POSTFIX_REGEX.find(text) ?: return null
        val cleanString = match.groupValues[1].replace(",", "")
        return cleanString.toDoubleOrNull()
    }

    private fun extractUpiVpa(text: String): String? {
        val match = UPI_VPA_REGEX.find(text) ?: return null
        val vpa = match.groupValues[1]
        // Filter out false positives like email address with standard domain extensions
        if (vpa.endsWith("@gmail.com", ignoreCase = true) ||
            vpa.endsWith("@yahoo.com", ignoreCase = true)
        ) {
            return null
        }
        return vpa
    }

    private fun extractUpiRefNo(text: String): String? {
        val prefMatch = PREF_12_DIGIT_REGEX.find(text)
        if (prefMatch != null) {
            return prefMatch.groupValues[1]
        }
        // Fallback: standalone 12 digits
        val standaloneMatches = STANDALONE_12_DIGIT_REGEX.findAll(text)
        for (m in standaloneMatches) {
            return m.groupValues[1]
        }
        return null
    }

    private fun extractRawMerchant(text: String, type: TransactionType, upiVpa: String?): String {
        // Pattern 1: Bank SMS with parenthesized merchant name after VPA:
        // "to VPA swiggy@icici (Swiggy)" or "to VPA 9876543210@ybl (Ramesh Kumar)"
        val vpaWithParenRegex = Regex("to VPA\\s+[^(\\n]+?\\s*\\(([^)\\n]+)\\)", RegexOption.IGNORE_CASE)
        val vpaWithParenMatch = vpaWithParenRegex.find(text)
        if (vpaWithParenMatch != null) {
            val name = vpaWithParenMatch.groupValues[1].trim()
            if (name.isNotBlank()) return cleanMerchantString(name)
        }

        // Pattern 2: "Info: UPI/<Ref>/<Merchant>" or "UPI/<Ref>/<Merchant>"
        val upiInfoRegex = Regex("(?:Info:\\s*)?UPI\\/[0-9]+\\/([^\\/\\n]+)", RegexOption.IGNORE_CASE)
        val upiInfoMatch = upiInfoRegex.find(text)
        if (upiInfoMatch != null) {
            val name = upiInfoMatch.groupValues[1].trim()
            if (name.isNotBlank()) return cleanMerchantString(name)
        }

        // Pattern 3: "at <Merchant> on" or "at <Merchant> using" or "at <Merchant> dated"
        val atMerchantRegex = Regex("(?i)\\bat\\s+([A-Za-z0-9\\s.&'\\-_]+?)\\s+(?:on|using|via|dated|ref|\\.)")
        val atMatch = atMerchantRegex.find(text)
        if (atMatch != null) {
            val name = atMatch.groupValues[1].trim()
            if (name.isNotBlank() && !name.equals("VPA", ignoreCase = true)) {
                return cleanMerchantString(name)
            }
        }

        // Pattern 4: Credit / Refund: "refund from <Merchant>", "transfer from <Merchant>", "received from <Merchant>"
        val fromRegex = Regex("(?i)(?:refund from|transfer from|received from|from)\\s+([A-Za-z0-9\\s&'\\-_]+?)(?:\\s+(?:on|using|via|in|ref|dated|by)|\\.|$)")
        val fromMatch = fromRegex.find(text)
        if (fromMatch != null) {
            val name = fromMatch.groupValues[1].trim()
            if (name.isNotBlank()) return cleanMerchantString(name)
        }

        // Pattern 5: UPI App format: "Paid ₹60 to <Merchant>" or "Sent ₹150 to <Merchant>"
        val paidToRegex = Regex("(?i)(?:paid|sent)\\s+(?:rs\\.?|inr|₹)?\\s*[0-9.,]+\\s+to\\s+([A-Za-z0-9\\s&'\\-_]+?)(?:\\s+(?:on|using|via|from)|\\.|$)")
        val paidToMatch = paidToRegex.find(text)
        if (paidToMatch != null) {
            val name = paidToMatch.groupValues[1].trim()
            if (name.isNotBlank()) return cleanMerchantString(name)
        }

        // Pattern 6: "You paid <Merchant> ₹60"
        val youPaidRegex = Regex("(?i)you paid\\s+([A-Za-z0-9\\s&'\\-_]+?)\\s+(?:rs\\.?|inr|₹)")
        val youPaidMatch = youPaidRegex.find(text)
        if (youPaidMatch != null) {
            val name = youPaidMatch.groupValues[1].trim()
            if (name.isNotBlank()) return cleanMerchantString(name)
        }

        // Pattern 7: "to VPA <VPA>" fallback
        val toVpaRegex = Regex("(?i)to VPA\\s+([A-Za-z0-9.\\-_@]+)")
        val toVpaMatch = toVpaRegex.find(text)
        if (toVpaMatch != null) {
            val candidate = toVpaMatch.groupValues[1].trim()
            if (candidate.isNotBlank()) return cleanMerchantString(candidate)
        }

        // Pattern 8: "to <Merchant> on" or "transfer to <Merchant> Ref"
        val toMerchantRegex = Regex("(?i)(?:transfer to|to)\\s+([A-Za-z0-9\\s&'\\-_]+?)(?:\\s+(?:on|using|via|dated|ref)|\\.|$)")
        val toMatch = toMerchantRegex.find(text)
        if (toMatch != null) {
            val name = toMatch.groupValues[1].trim()
            if (name.isNotBlank() && !name.equals("VPA", ignoreCase = true)) {
                return cleanMerchantString(name)
            }
        }

        // Pattern 9: "for <Merchant>" or "towards <Merchant>" (e.g. SIP, Bill, Rent)
        val forMerchantRegex = Regex("(?i)(?:debited for|for|towards)\\s+([A-Za-z0-9\\s&'\\-_]+?)(?:\\s+(?:on|using|via|dated|ref|sip|order)|\\.|$)")
        val forMatch = forMerchantRegex.find(text)
        if (forMatch != null) {
            val name = forMatch.groupValues[1].trim()
            if (name.isNotBlank()) return cleanMerchantString(name)
        }

        // If upiVpa exists, use clean VPA prefix as fallback
        if (upiVpa != null) {
            val prefix = upiVpa.substringBefore('@')
            if (prefix.isNotBlank()) return prefix
        }

        return if (type == TransactionType.DEBIT) "Debit Transaction" else "Credit Transaction"
    }

    private fun cleanMerchantString(raw: String): String {
        return raw
            .substringBefore('.')
            .replace(Regex("(?i)^(?:the\\s+|vpa\\s+)"), "")
            .replace(Regex("(?i)\\s+(?:successful|completed|via|on|ref|using|a\\/c|upi).*$"), "")
            .trim()
            .trimEnd('.', ',', '-', ':')
    }

    private fun resolveSource(senderOrPkg: String?, text: String): String {
        val s = (senderOrPkg ?: "").uppercase(Locale.ROOT)
        val body = text.uppercase(Locale.ROOT)

        // Sender header / package has highest authority
        if (s.isNotBlank()) {
            when {
                s.contains("GPAY") || s.contains("PAISA") -> return "Google Pay"
                s.contains("PHONEPE") -> return "PhonePe"
                s.contains("PAYTM") -> return "Paytm"
                s.contains("AMAZON") -> return "Amazon Pay"
                s.contains("HDFC") -> return "HDFC Bank"
                s.contains("SBI") -> return "SBI"
                s.contains("ICICI") -> return "ICICI Bank"
                s.contains("AXIS") -> return "Axis Bank"
                s.contains("KOTAK") -> return "Kotak Bank"
                s.contains("INDUS") -> return "IndusInd Bank"
                s.contains("PNB") -> return "PNB"
                s.contains("BOB") || s.contains("BARODA") -> return "Bank of Baroda"
            }
        }

        // Fallback to body content mentions
        return when {
            body.contains("HDFC BANK") -> "HDFC Bank"
            body.contains("STATE BANK") || body.contains("SBI") -> "SBI"
            body.contains("ICICI BANK") -> "ICICI Bank"
            body.contains("AXIS BANK") -> "Axis Bank"
            body.contains("KOTAK BANK") -> "Kotak Bank"
            body.contains("GOOGLE PAY") -> "Google Pay"
            body.contains("PHONEPE") -> "PhonePe"
            body.contains("PAYTM") -> "Paytm"
            body.contains("AMAZON PAY") -> "Amazon Pay"
            else -> "Bank / UPI"
        }
    }
}
