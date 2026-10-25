package com.example.pulseledger.data.repository

import android.content.Context
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.dao.UpcomingMandateDao
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.UpcomingMandateEntity
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.tier2.DeterministicRuleEngine
import com.example.pulseledger.data.preference.IntelligencePreferences
import com.example.pulseledger.data.remote.GeminiClassifierClient
import com.example.pulseledger.data.security.KeyStorageProvider
import com.example.pulseledger.data.security.SecureKeyStorage
import com.example.pulseledger.domain.notification.AndroidNotificationTrigger
import com.example.pulseledger.domain.notification.NotificationTrigger
import com.example.pulseledger.domain.parser.ParsedTransaction
import com.example.pulseledger.domain.parser.TransactionParser
import com.example.pulseledger.ui.model.PulseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class ResolutionTier {
    TIER_1_MEMORY,
    TIER_2_DETERMINISTIC,
    TIER_3_GEMINI_AI,
    TIER_4_PENDING_USER_TAP,
    DEDUPLICATED_MERGE,
    COMMERCIAL_REFUND_OFFSET,
    P2P_SPLIT_BILL_DETECTED,
    UPCOMING_MANDATE_INTERCEPTED,
    TRANSFER_NEUTRAL
}

data class WaterfallResult(
    val transaction: TransactionEntity,
    val resolutionTier: ResolutionTier,
    val isDeduplicatedMerge: Boolean = false
)

/**
 * The Central 4-Tier Categorization Waterfall Coordinator with Step 8.5 Intelligence:
 *
 * Tier 1: Local Room DB exact merchant memory lookup (100% accuracy, 0ms, 0 tokens)
 * Tier 2: Deterministic on-device keyword/prefix dictionary matcher (<1ms, 0 tokens)
 * Tier 3: Scrubbed Gemini REST API classifier (confidence >= 0.85 & !isP2pPersonName)
 * Tier 4: Instant 1-Tap Android Actionable Notification fallback for P2P / unverified
 *
 * Intelligence Features:
 * - Pre-Debit Mandate Interceptor (AutoPay, e-mandate)
 * - Commercial Refund Reconciler (links CREDIT to 14-day DEBIT & deducts net spend)
 * - P2P Split-Bill Linker (detects 24h debit, fires 1-tap offset push)
 * - Closed-Loop Wallet De-Duplicator (TRANSFER_NEUTRAL for wallets & CC payments)
 * - Active Trip / Event Mode tagging
 */
class ExpenseWaterfallRepository(
    private val transactionDao: TransactionDao,
    private val merchantMemoryDao: MerchantMemoryDao,
    private val upcomingMandateDao: UpcomingMandateDao,
    private val keyStorage: KeyStorageProvider,
    private val geminiClassifier: GeminiClassifierClient = GeminiClassifierClient(keyStorage),
    private val notificationTrigger: NotificationTrigger? = null,
    private val intelligencePrefs: IntelligencePreferences? = null
) {

    constructor(
        context: Context,
        transactionDao: TransactionDao,
        merchantMemoryDao: MerchantMemoryDao,
        upcomingMandateDao: UpcomingMandateDao
    ) : this(
        transactionDao = transactionDao,
        merchantMemoryDao = merchantMemoryDao,
        upcomingMandateDao = upcomingMandateDao,
        keyStorage = SecureKeyStorage(context),
        geminiClassifier = GeminiClassifierClient(SecureKeyStorage(context)),
        notificationTrigger = AndroidNotificationTrigger(context),
        intelligencePrefs = IntelligencePreferences.getInstance(context)
    )

    constructor(
        context: Context,
        transactionDao: TransactionDao,
        merchantMemoryDao: MerchantMemoryDao
    ) : this(
        transactionDao = transactionDao,
        merchantMemoryDao = merchantMemoryDao,
        upcomingMandateDao = PulseDatabase.getInstance(context).upcomingMandateDao(),
        keyStorage = SecureKeyStorage(context),
        geminiClassifier = GeminiClassifierClient(SecureKeyStorage(context)),
        notificationTrigger = AndroidNotificationTrigger(context),
        intelligencePrefs = IntelligencePreferences.getInstance(context)
    )

    val allTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow().flowOn(Dispatchers.IO)

    val pendingTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getPendingTransactionsFlow().flowOn(Dispatchers.IO)

    val activeMandates: Flow<List<UpcomingMandateEntity>> =
        upcomingMandateDao.getActiveMandatesFlow().flowOn(Dispatchers.IO)

    /**
     * Ingests a raw notification or SMS string directly through the waterfall.
     */
    suspend fun processRawText(
        text: String,
        senderOrPackage: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): WaterfallResult? = withContext(Dispatchers.IO) {
        // Step 8.5 Pre-Debit Interceptor for Upcoming Mandates
        val mandate = TransactionParser.parseMandate(text, timestamp)
        if (mandate != null) {
            upcomingMandateDao.insertMandate(mandate)
            val mandateEntity = TransactionEntity(
                id = mandate.id,
                amount = mandate.amount,
                netAmount = mandate.amount,
                type = TransactionType.DEBIT,
                rawMerchant = mandate.merchant,
                displayMerchant = mandate.merchant,
                bankOrAppSource = "Scheduled Mandate",
                category = PulseCategory.BILLS_AND_UTILITIES.id,
                timestamp = mandate.dueDate,
                verificationState = VerificationState.VERIFIED_RULE
            )
            return@withContext WaterfallResult(
                transaction = mandateEntity,
                resolutionTier = ResolutionTier.UPCOMING_MANDATE_INTERCEPTED
            )
        }

        val parsed = TransactionParser.parse(text, senderOrPackage, timestamp) ?: return@withContext null
        processIncomingTransaction(parsed)
    }

    /**
     * Executes the full categorization waterfall with Step 8.5 intelligence.
     */
    suspend fun processIncomingTransaction(parsedTx: ParsedTransaction): WaterfallResult = withContext(Dispatchers.IO) {
        // Step 0: 3-Minute Sliding Window Deduplication Check
        val candidates = transactionDao.findWindowCandidates(
            amount = parsedTx.amount,
            type = parsedTx.type,
            timestamp = parsedTx.timestamp
        )

        val duplicateCandidate = candidates.firstOrNull { isSameTransaction(it, parsedTx) }
        if (duplicateCandidate != null) {
            val merged = mergeTransactions(duplicateCandidate, parsedTx)
            transactionDao.updateTransaction(merged)
            return@withContext WaterfallResult(
                transaction = merged,
                resolutionTier = ResolutionTier.DEDUPLICATED_MERGE,
                isDeduplicatedMerge = true
            )
        }

        val deterministicId = parsedTx.upiRefNo
            ?: "tx_${parsedTx.timestamp}_${(parsedTx.amount * 100).toLong()}_${abs(parsedTx.rawMerchant.hashCode())}"
        val vpaKey = parsedTx.upiVpa?.trim()?.lowercase(java.util.Locale.ROOT)
        val normalizedMerchantKey = parsedTx.rawMerchant.trim().lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val lookupKey = DeterministicRuleEngine.extractLookupKey(parsedTx.rawMerchant, parsedTx.upiVpa)
        val activeEventTag = parsedTx.eventTag ?: intelligencePrefs?.activeEventTag?.value

        // Step 8.5 Feature 4: Closed-Loop Wallet De-Duplicator (TRANSFER_NEUTRAL)
        if (parsedTx.type == TransactionType.TRANSFER_NEUTRAL) {
            val neutralEntity = TransactionEntity(
                id = deterministicId,
                amount = parsedTx.amount,
                netAmount = 0.0,
                type = TransactionType.TRANSFER_NEUTRAL,
                rawMerchant = parsedTx.rawMerchant,
                displayMerchant = parsedTx.rawMerchant,
                upiVpa = parsedTx.upiVpa,
                upiRefNo = parsedTx.upiRefNo,
                bankOrAppSource = parsedTx.bankOrAppSource,
                category = "Transfers",
                timestamp = parsedTx.timestamp,
                verificationState = VerificationState.VERIFIED_RULE,
                eventTag = activeEventTag
            )
            transactionDao.insertTransaction(neutralEntity)
            return@withContext WaterfallResult(neutralEntity, ResolutionTier.TRANSFER_NEUTRAL)
        }

        // Step 8.5 Feature 3A: Commercial Refund Reconciler
        if (parsedTx.type == TransactionType.CREDIT) {
            val isTier2Commercial = DeterministicRuleEngine.isTier2Merchant(parsedTx.rawMerchant, parsedTx.upiVpa)
            val tier1Commercial = merchantMemoryDao.getMerchantMemory(lookupKey)

            if (isTier2Commercial || tier1Commercial != null) {
                val vpaPrefix = parsedTx.upiVpa?.substringBefore('@') ?: ""
                val recentDebit = transactionDao.findRecentDebitForMerchant(
                    merchantKeyword = lookupKey,
                    upiVpaPrefix = vpaPrefix,
                    currentTimestamp = parsedTx.timestamp
                )

                if (recentDebit != null) {
                    val refundEntity = TransactionEntity(
                        id = deterministicId,
                        amount = parsedTx.amount,
                        netAmount = parsedTx.amount,
                        type = TransactionType.CREDIT,
                        rawMerchant = parsedTx.rawMerchant,
                        displayMerchant = recentDebit.displayMerchant,
                        upiVpa = parsedTx.upiVpa,
                        upiRefNo = parsedTx.upiRefNo,
                        bankOrAppSource = parsedTx.bankOrAppSource,
                        category = recentDebit.category,
                        timestamp = parsedTx.timestamp,
                        verificationState = VerificationState.VERIFIED_RULE,
                        parentTxId = recentDebit.id,
                        isRefund = true,
                        eventTag = recentDebit.eventTag ?: activeEventTag
                    )
                    transactionDao.reduceNetAmount(recentDebit.id, parsedTx.amount)
                    transactionDao.insertTransaction(refundEntity)
                    return@withContext WaterfallResult(refundEntity, ResolutionTier.COMMERCIAL_REFUND_OFFSET)
                }
            }

            // Step 8.5 Feature 3B: P2P Split-Bill Linker
            if (parsedTx.amount < 5000.0) {
                val recentDebit = transactionDao.findRecentEligibleDebitForSplit(
                    creditAmount = parsedTx.amount,
                    currentTimestamp = parsedTx.timestamp
                )
                if (recentDebit != null) {
                    val splitCreditEntity = TransactionEntity(
                        id = deterministicId,
                        amount = parsedTx.amount,
                        netAmount = parsedTx.amount,
                        type = TransactionType.CREDIT,
                        rawMerchant = parsedTx.rawMerchant,
                        displayMerchant = parsedTx.rawMerchant,
                        upiVpa = parsedTx.upiVpa,
                        upiRefNo = parsedTx.upiRefNo,
                        bankOrAppSource = parsedTx.bankOrAppSource,
                        category = recentDebit.category,
                        timestamp = parsedTx.timestamp,
                        verificationState = VerificationState.PENDING_USER_TAP,
                        parentTxId = recentDebit.id,
                        isRefund = false,
                        eventTag = recentDebit.eventTag ?: activeEventTag
                    )
                    transactionDao.insertTransaction(splitCreditEntity)
                    notificationTrigger?.fireSplitBillNotification(splitCreditEntity, recentDebit)
                    return@withContext WaterfallResult(splitCreditEntity, ResolutionTier.P2P_SPLIT_BILL_DETECTED)
                }
            }
        }

        // Tier 1 Check: Local Room DB merchant_memory exact match
        var matchedKey = vpaKey
        var tier1Memory = if (!vpaKey.isNullOrBlank()) merchantMemoryDao.getMerchantMemory(vpaKey) else null

        if (tier1Memory == null) {
            tier1Memory = merchantMemoryDao.getMerchantMemory(lookupKey)
            if (tier1Memory != null) matchedKey = lookupKey
        }
        if (tier1Memory == null) {
            tier1Memory = merchantMemoryDao.getMerchantMemory(normalizedMerchantKey)
            if (tier1Memory != null) matchedKey = normalizedMerchantKey
        }

        if (tier1Memory != null) {
            val entity = TransactionEntity(
                id = deterministicId,
                amount = parsedTx.amount,
                netAmount = parsedTx.amount,
                type = parsedTx.type,
                rawMerchant = parsedTx.rawMerchant,
                displayMerchant = tier1Memory.customAlias ?: parsedTx.rawMerchant,
                upiVpa = parsedTx.upiVpa,
                upiRefNo = parsedTx.upiRefNo,
                bankOrAppSource = parsedTx.bankOrAppSource,
                category = tier1Memory.category,
                timestamp = parsedTx.timestamp,
                verificationState = VerificationState.VERIFIED_MEMORY,
                eventTag = activeEventTag
            )
            transactionDao.insertTransaction(entity)
            merchantMemoryDao.rememberMerchant(
                lookupKey = matchedKey ?: lookupKey,
                category = tier1Memory.category,
                customAlias = tier1Memory.customAlias,
                timestamp = parsedTx.timestamp
            )
            return@withContext WaterfallResult(entity, ResolutionTier.TIER_1_MEMORY)
        }

        // Tier 2 Check: Deterministic On-Device Rule Matcher
        val tier2Match = DeterministicRuleEngine.match(parsedTx.rawMerchant, parsedTx.upiVpa)
        if (tier2Match != null) {
            val entity = TransactionEntity(
                id = deterministicId,
                amount = parsedTx.amount,
                netAmount = parsedTx.amount,
                type = parsedTx.type,
                rawMerchant = parsedTx.rawMerchant,
                displayMerchant = tier2Match.cleanMerchantName,
                upiVpa = parsedTx.upiVpa,
                upiRefNo = parsedTx.upiRefNo,
                bankOrAppSource = parsedTx.bankOrAppSource,
                category = tier2Match.category.id,
                timestamp = parsedTx.timestamp,
                verificationState = VerificationState.VERIFIED_RULE,
                eventTag = activeEventTag
            )
            transactionDao.insertTransaction(entity)
            return@withContext WaterfallResult(entity, ResolutionTier.TIER_2_DETERMINISTIC)
        }

        // Tier 3 Check: Gemini Structured REST Classifier
        val isLocalPersonName = DeterministicRuleEngine.isProbablePersonalName(parsedTx.rawMerchant)

        if (!isLocalPersonName && !keyStorage.getGeminiApiKey().isNullOrBlank()) {
            val geminiResult = geminiClassifier.classify(
                rawMerchant = parsedTx.rawMerchant,
                upiVpa = parsedTx.upiVpa,
                amount = parsedTx.amount,
                timestamp = parsedTx.timestamp
            )

            if (geminiResult != null && !geminiResult.isP2pPersonName && geminiResult.confidence >= 0.85) {
                val resolvedCategory = PulseCategory.fromString(geminiResult.category)
                val cleanName = geminiResult.cleanMerchantName.ifBlank { parsedTx.rawMerchant }

                val entity = TransactionEntity(
                    id = deterministicId,
                    amount = parsedTx.amount,
                    netAmount = parsedTx.amount,
                    type = parsedTx.type,
                    rawMerchant = parsedTx.rawMerchant,
                    displayMerchant = cleanName,
                    upiVpa = parsedTx.upiVpa,
                    upiRefNo = parsedTx.upiRefNo,
                    bankOrAppSource = parsedTx.bankOrAppSource,
                    category = resolvedCategory.id,
                    timestamp = parsedTx.timestamp,
                    verificationState = VerificationState.VERIFIED_AI,
                    eventTag = activeEventTag
                )
                transactionDao.insertTransaction(entity)

                merchantMemoryDao.rememberMerchant(
                    lookupKey = lookupKey,
                    category = resolvedCategory.id,
                    customAlias = cleanName,
                    timestamp = parsedTx.timestamp
                )

                return@withContext WaterfallResult(entity, ResolutionTier.TIER_3_GEMINI_AI)
            }
        }

        // Tier 4 Fallback: Unverified P2P or low confidence -> Route to PENDING_USER_TAP
        val pendingEntity = TransactionEntity(
            id = deterministicId,
            amount = parsedTx.amount,
            netAmount = parsedTx.amount,
            type = parsedTx.type,
            rawMerchant = parsedTx.rawMerchant,
            displayMerchant = parsedTx.rawMerchant,
            upiVpa = parsedTx.upiVpa,
            upiRefNo = parsedTx.upiRefNo,
            bankOrAppSource = parsedTx.bankOrAppSource,
            category = PulseCategory.UNCATEGORIZED_P2P.id,
            timestamp = parsedTx.timestamp,
            verificationState = VerificationState.PENDING_USER_TAP,
            eventTag = activeEventTag
        )
        transactionDao.insertTransaction(pendingEntity)

        notificationTrigger?.fireQuickTagNotification(pendingEntity)

        return@withContext WaterfallResult(pendingEntity, ResolutionTier.TIER_4_PENDING_USER_TAP)
    }

    suspend fun resolveAndRememberMerchant(
        txId: String,
        lookupKey: String,
        category: String,
        customAlias: String? = null
    ) = withContext(Dispatchers.IO) {
        transactionDao.resolveAndRememberMerchant(txId, lookupKey, category, customAlias)
    }

    suspend fun linkSplitBill(
        creditTxId: String,
        parentDebitId: String,
        offsetAmount: Double
    ) = withContext(Dispatchers.IO) {
        transactionDao.linkSplitBill(creditTxId, parentDebitId, offsetAmount)
    }

    suspend fun markMandatePaid(id: String) = withContext(Dispatchers.IO) {
        upcomingMandateDao.markAsPaid(id)
    }

    suspend fun dismissMandate(id: String) = withContext(Dispatchers.IO) {
        upcomingMandateDao.dismissMandate(id)
    }

    private fun isSameTransaction(candidate: TransactionEntity, incoming: ParsedTransaction): Boolean {
        if (!candidate.upiRefNo.isNullOrBlank() && !incoming.upiRefNo.isNullOrBlank()) {
            return candidate.upiRefNo == incoming.upiRefNo
        }
        if (!candidate.upiVpa.isNullOrBlank() && !incoming.upiVpa.isNullOrBlank()) {
            if (candidate.upiVpa.equals(incoming.upiVpa, ignoreCase = true)) return true
        }
        val cNorm = candidate.rawMerchant.trim().lowercase()
        val pNorm = incoming.rawMerchant.trim().lowercase()
        if (cNorm == pNorm || cNorm.contains(pNorm) || pNorm.contains(cNorm)) return true

        return true
    }

    private fun mergeTransactions(existing: TransactionEntity, incoming: ParsedTransaction): TransactionEntity {
        val mergedUpiRef = existing.upiRefNo ?: incoming.upiRefNo
        val mergedUpiVpa = existing.upiVpa ?: incoming.upiVpa

        val mergedSource = if (existing.bankOrAppSource.contains(incoming.bankOrAppSource, ignoreCase = true)) {
            existing.bankOrAppSource
        } else {
            "${existing.bankOrAppSource} + ${incoming.bankOrAppSource}"
        }

        val betterMerchant = if (existing.displayMerchant.startsWith("Debit Transaction", ignoreCase = true)) {
            incoming.rawMerchant
        } else {
            existing.displayMerchant
        }

        return existing.copy(
            displayMerchant = betterMerchant,
            upiVpa = mergedUpiVpa,
            upiRefNo = mergedUpiRef,
            bankOrAppSource = mergedSource
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ExpenseWaterfallRepository? = null

        fun getInstance(context: Context): ExpenseWaterfallRepository {
            return INSTANCE ?: synchronized(this) {
                val db = PulseDatabase.getInstance(context.applicationContext)
                val instance = ExpenseWaterfallRepository(
                    context = context.applicationContext,
                    transactionDao = db.transactionDao(),
                    merchantMemoryDao = db.merchantMemoryDao(),
                    upcomingMandateDao = db.upcomingMandateDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
