package com.example.pulseledger.data.repository

import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.local.model.DailySpend
import com.example.pulseledger.data.remote.GeminiClassificationResult
import com.example.pulseledger.data.remote.GeminiClassifierClient
import com.example.pulseledger.data.security.KeyStorageProvider
import com.example.pulseledger.domain.parser.ParsedTransaction
import com.example.pulseledger.ui.model.PulseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseWaterfallRepositoryTest {

    private class FakeTransactionDao : TransactionDao {
        val database = mutableMapOf<String, TransactionEntity>()

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            database[transaction.id] = transaction
        }

        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            transactions.forEach { database[it.id] = it }
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            database[transaction.id] = transaction
        }

        override suspend fun getTransactionById(id: String): TransactionEntity? = database[id]

        override suspend fun deleteTransaction(id: String): Int = if (database.remove(id) != null) 1 else 0

        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> =
            MutableStateFlow(database.values.toList())

        override fun getTransactionsByDateRangeFlow(
            startTimestamp: Long,
            endTimestamp: Long
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun getTransactionsByCategoryFlow(category: String): Flow<List<TransactionEntity>> =
            MutableStateFlow(emptyList())

        override fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>> =
            MutableStateFlow(database.values.filter { it.verificationState == VerificationState.PENDING_USER_TAP })

        override fun getPendingCountFlow(): Flow<Int> =
            MutableStateFlow(database.values.count { it.verificationState == VerificationState.PENDING_USER_TAP })

        override fun getCategorySpendTotalsFlow(
            startTimestamp: Long,
            endTimestamp: Long
        ): Flow<List<CategorySpend>> = MutableStateFlow(emptyList())

        override fun getDailySpendTotalsFlow(startTimestamp: Long): Flow<List<DailySpend>> =
            MutableStateFlow(emptyList())

        override suspend fun findSlidingDuplicate(
            amount: Double,
            upiRefNo: String?,
            rawMerchant: String,
            timestamp: Long
        ): TransactionEntity? = null

        override suspend fun findWindowCandidates(
            amount: Double,
            type: TransactionType,
            timestamp: Long
        ): List<TransactionEntity> {
            return database.values.filter {
                it.amount == amount &&
                it.type == type &&
                it.timestamp in (timestamp - 180_000)..(timestamp + 180_000)
            }
        }

        override suspend fun updateTransactionResolution(txId: String, category: String, customAlias: String?): Int {
            val existing = database[txId] ?: return 0
            database[txId] = existing.copy(
                category = category,
                verificationState = VerificationState.VERIFIED_USER,
                displayMerchant = customAlias ?: existing.displayMerchant
            )
            return 1
        }

        override suspend fun retroactivelyUpdatePendingTransactions(
            lookupKey: String,
            category: String,
            customAlias: String?
        ): Int = 0

        override suspend fun upsertMerchantMemory(
            lookupKey: String,
            category: String,
            customAlias: String?,
            timestamp: Long
        ): Long = 1
    }

    private class FakeMemoryDao : MerchantMemoryDao {
        val memory = mutableMapOf<String, MerchantMemoryEntity>()

        override suspend fun getMerchantMemory(lookupKey: String): MerchantMemoryEntity? = memory[lookupKey]

        override fun getAllMerchantMemoryFlow(): Flow<List<MerchantMemoryEntity>> =
            MutableStateFlow(memory.values.toList())

        override suspend fun upsert(entity: MerchantMemoryEntity) {
            memory[entity.lookupKey] = entity
        }

        override suspend fun update(entity: MerchantMemoryEntity) {
            memory[entity.lookupKey] = entity
        }

        override suspend fun delete(lookupKey: String): Int = if (memory.remove(lookupKey) != null) 1 else 0

        override suspend fun getMemoryCount(): Int = memory.size
    }

    private val fakeKeyStorage = object : KeyStorageProvider {
        override fun getGeminiApiKey(): String? = "test_api_key"
        override fun setGeminiApiKey(apiKey: String) {}
        override fun clearGeminiApiKey() {}
    }

    private class FakeGeminiClassifier(
        keyStorage: KeyStorageProvider,
        var mockResult: GeminiClassificationResult? = null
    ) : GeminiClassifierClient(keyStorage) {
        override suspend fun classify(
            rawMerchant: String,
            upiVpa: String?,
            amount: Double,
            timestamp: Long
        ): GeminiClassificationResult? {
            return mockResult
        }
    }

    @Test
    fun testTier1MemoryResolution() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()
        val gemini = FakeGeminiClassifier(fakeKeyStorage)

        // Seed Tier 1 memory
        memDao.memory["localchai@upi"] = MerchantMemoryEntity(
            lookupKey = "localchai@upi",
            category = PulseCategory.FOOD_AND_DINING.id,
            customAlias = "Sharma Chai Wala",
            transactionCount = 5
        )

        val repository = ExpenseWaterfallRepository(
            transactionDao = txDao,
            merchantMemoryDao = memDao,
            keyStorage = fakeKeyStorage,
            geminiClassifier = gemini
        )

        val parsed = ParsedTransaction(
            amount = 40.0,
            type = TransactionType.DEBIT,
            rawMerchant = "Sharma Ji",
            upiVpa = "localchai@upi",
            bankOrAppSource = "Google Pay"
        )

        val result = repository.processIncomingTransaction(parsed)

        assertEquals(ResolutionTier.TIER_1_MEMORY, result.resolutionTier)
        assertEquals(VerificationState.VERIFIED_MEMORY, result.transaction.verificationState)
        assertEquals("Sharma Chai Wala", result.transaction.displayMerchant)
        assertEquals(PulseCategory.FOOD_AND_DINING.id, result.transaction.category)
    }

    @Test
    fun testTier2DeterministicRuleResolution() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()
        val gemini = FakeGeminiClassifier(fakeKeyStorage)

        val repository = ExpenseWaterfallRepository(
            transactionDao = txDao,
            merchantMemoryDao = memDao,
            keyStorage = fakeKeyStorage,
            geminiClassifier = gemini
        )

        val parsed = ParsedTransaction(
            amount = 350.0,
            type = TransactionType.DEBIT,
            rawMerchant = "ZEPTO QUICK BANGALORE",
            upiVpa = "zepto@axis",
            bankOrAppSource = "PhonePe"
        )

        val result = repository.processIncomingTransaction(parsed)

        assertEquals(ResolutionTier.TIER_2_DETERMINISTIC, result.resolutionTier)
        assertEquals(VerificationState.VERIFIED_RULE, result.transaction.verificationState)
        assertEquals("Zepto", result.transaction.displayMerchant)
        assertEquals(PulseCategory.GROCERIES.id, result.transaction.category)
    }

    @Test
    fun testTier3GeminiAiResolutionAndMemoryCaching() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()
        val gemini = FakeGeminiClassifier(fakeKeyStorage)

        // Mock Gemini response for a niche commercial store
        gemini.mockResult = GeminiClassificationResult(
            category = "Shopping",
            cleanMerchantName = "FabIndia Handicrafts",
            isP2pPersonName = false,
            confidence = 0.94
        )

        val repository = ExpenseWaterfallRepository(
            transactionDao = txDao,
            merchantMemoryDao = memDao,
            keyStorage = fakeKeyStorage,
            geminiClassifier = gemini
        )

        val parsed = ParsedTransaction(
            amount = 1890.0,
            type = TransactionType.DEBIT,
            rawMerchant = "FABINDIA ETHNIC LTD",
            upiVpa = "fabindia@hdfc",
            bankOrAppSource = "HDFC Bank"
        )

        val result = repository.processIncomingTransaction(parsed)

        assertEquals(ResolutionTier.TIER_3_GEMINI_AI, result.resolutionTier)
        assertEquals(VerificationState.VERIFIED_AI, result.transaction.verificationState)
        assertEquals("FabIndia Handicrafts", result.transaction.displayMerchant)
        assertEquals(PulseCategory.SHOPPING.id, result.transaction.category)

        // Tier 3 MUST cache merchant in Tier 1 memory for zero-cost repeat lookups!
        val cached = memDao.getMerchantMemory("fabindia")
        assertNotNull(cached)
        assertEquals(PulseCategory.SHOPPING.id, cached!!.category)
    }

    @Test
    fun testTier4PendingUserTapForHumanName() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()
        val gemini = FakeGeminiClassifier(fakeKeyStorage)

        val repository = ExpenseWaterfallRepository(
            transactionDao = txDao,
            merchantMemoryDao = memDao,
            keyStorage = fakeKeyStorage,
            geminiClassifier = gemini
        )

        // Human name without commercial keywords -> strictly P2P Zero-Guessing Rule
        val parsed = ParsedTransaction(
            amount = 1250.0,
            type = TransactionType.DEBIT,
            rawMerchant = "Suresh Yadav",
            upiVpa = "suresh.yadav@upi",
            bankOrAppSource = "Google Pay"
        )

        val result = repository.processIncomingTransaction(parsed)

        assertEquals(ResolutionTier.TIER_4_PENDING_USER_TAP, result.resolutionTier)
        assertEquals(VerificationState.PENDING_USER_TAP, result.transaction.verificationState)
        assertEquals(PulseCategory.UNCATEGORIZED_P2P.id, result.transaction.category)
    }
}
