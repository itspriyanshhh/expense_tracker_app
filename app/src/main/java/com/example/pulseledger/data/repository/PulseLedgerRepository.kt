package com.example.pulseledger.data.repository

import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.local.model.DailySpend
import com.example.pulseledger.data.local.tier2.DeterministicRuleEngine
import com.example.pulseledger.ui.model.PulseCategory
import com.example.pulseledger.domain.parser.DeduplicationManager
import com.example.pulseledger.domain.parser.DeduplicationResult
import com.example.pulseledger.domain.parser.ParsedTransaction
import com.example.pulseledger.domain.parser.TransactionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class RawIngestedTransaction(
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val rawMerchant: String,
    val upiVpa: String? = null,
    val upiRefNo: String? = null,
    val bankOrAppSource: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PulseLedgerRepository(
    private val transactionDao: TransactionDao,
    private val merchantMemoryDao: MerchantMemoryDao,
    val deduplicationManager: DeduplicationManager = DeduplicationManager(transactionDao, merchantMemoryDao)
) {

    val allTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow().flowOn(Dispatchers.IO)

    val pendingTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getPendingTransactionsFlow().flowOn(Dispatchers.IO)

    val pendingCount: Flow<Int> =
        transactionDao.getPendingCountFlow().flowOn(Dispatchers.IO)

    val allMerchantMemory: Flow<List<MerchantMemoryEntity>> =
        merchantMemoryDao.getAllMerchantMemoryFlow().flowOn(Dispatchers.IO)

    fun getCategorySpend(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpend>> =
        transactionDao.getCategorySpendTotalsFlow(startTimestamp, endTimestamp).flowOn(Dispatchers.IO)

    fun getDailySpend(startTimestamp: Long): Flow<List<DailySpend>> =
        transactionDao.getDailySpendTotalsFlow(startTimestamp).flowOn(Dispatchers.IO)

    /**
     * Parses raw incoming notification/SMS text and runs through the 3-minute deduplication
     * and Tier 1 / Tier 2 categorization engine.
     */
    suspend fun ingestRawText(
        text: String,
        senderOrPackage: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): DeduplicationResult? = withContext(Dispatchers.IO) {
        val parsed = TransactionParser.parse(text, senderOrPackage, timestamp) ?: return@withContext null
        deduplicationManager.process(parsed)
    }

    /**
     * Executes Tier 1 & Tier 2 categorization on an ingested transaction and commits to Room DB.
     * Enforces the 3-minute sliding window deduplication to prevent double counting.
     */
    suspend fun ingestTransaction(raw: RawIngestedTransaction): TransactionEntity = withContext(Dispatchers.IO) {
        // 1. Sliding Window Deduplication (± 180 seconds)
        val duplicate = transactionDao.findSlidingDuplicate(
            amount = raw.amount,
            upiRefNo = raw.upiRefNo,
            rawMerchant = raw.rawMerchant,
            timestamp = raw.timestamp
        )

        if (duplicate != null) {
            // Transaction already exists; return existing entity without creating duplicate
            return@withContext duplicate
        }

        // 2. Tier 1: Local Room DB merchant_memory exact match
        val lookupKey = DeterministicRuleEngine.extractLookupKey(raw.rawMerchant, raw.upiVpa)
        val tier1Memory = merchantMemoryDao.getMerchantMemory(lookupKey)

        if (tier1Memory != null) {
            val entity = TransactionEntity(
                id = raw.id,
                amount = raw.amount,
                type = raw.type,
                rawMerchant = raw.rawMerchant,
                displayMerchant = tier1Memory.customAlias ?: raw.rawMerchant,
                upiVpa = raw.upiVpa,
                upiRefNo = raw.upiRefNo,
                bankOrAppSource = raw.bankOrAppSource,
                category = tier1Memory.category,
                timestamp = raw.timestamp,
                verificationState = VerificationState.VERIFIED_MEMORY
            )
            transactionDao.insertTransaction(entity)
            // Increment memory count asynchronously
            merchantMemoryDao.rememberMerchant(lookupKey, tier1Memory.category, tier1Memory.customAlias, raw.timestamp)
            return@withContext entity
        }

        // 3. Tier 2: Deterministic Rule Engine (On-device keyword & prefix match)
        val tier2Match = DeterministicRuleEngine.match(raw.rawMerchant, raw.upiVpa)
        if (tier2Match != null) {
            val entity = TransactionEntity(
                id = raw.id,
                amount = raw.amount,
                type = raw.type,
                rawMerchant = raw.rawMerchant,
                displayMerchant = tier2Match.cleanMerchantName,
                upiVpa = raw.upiVpa,
                upiRefNo = raw.upiRefNo,
                bankOrAppSource = raw.bankOrAppSource,
                category = tier2Match.category.id,
                timestamp = raw.timestamp,
                verificationState = VerificationState.VERIFIED_RULE
            )
            transactionDao.insertTransaction(entity)
            return@withContext entity
        }

        // 4. P2P Zero-Guessing Rule Fallback:
        // If it looks like a person's name or is unclassified, strictly route to PENDING_USER_TAP
        val isPerson = DeterministicRuleEngine.isProbablePersonalName(raw.rawMerchant)
        val entity = TransactionEntity(
            id = raw.id,
            amount = raw.amount,
            type = raw.type,
            rawMerchant = raw.rawMerchant,
            displayMerchant = raw.rawMerchant,
            upiVpa = raw.upiVpa,
            upiRefNo = raw.upiRefNo,
            bankOrAppSource = raw.bankOrAppSource,
            category = PulseCategory.UNCATEGORIZED_P2P.id,
            timestamp = raw.timestamp,
            verificationState = VerificationState.PENDING_USER_TAP
        )
        transactionDao.insertTransaction(entity)
        return@withContext entity
    }

    /**
     * Resolves a pending transaction, updates Tier 1 memory, and retroactively updates
     * any existing pending transactions from the same vendor/payee.
     */
    suspend fun resolveAndRememberMerchant(
        txId: String,
        lookupKey: String,
        category: String,
        customAlias: String? = null
    ) = withContext(Dispatchers.IO) {
        transactionDao.resolveAndRememberMerchant(
            txId = txId,
            lookupKey = lookupKey,
            category = category,
            customAlias = customAlias
        )
    }
}
