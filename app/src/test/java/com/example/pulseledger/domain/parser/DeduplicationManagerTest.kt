package com.example.pulseledger.domain.parser

import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.local.model.DailySpend
import com.example.pulseledger.ui.model.PulseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicationManagerTest {

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
        ): Flow<List<TransactionEntity>> =
            MutableStateFlow(database.values.filter { it.timestamp in startTimestamp until endTimestamp })

        override fun getTransactionsByCategoryFlow(category: String): Flow<List<TransactionEntity>> =
            MutableStateFlow(database.values.filter { it.category == category })

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
        ): TransactionEntity? {
            return database.values.firstOrNull {
                it.amount == amount &&
                it.timestamp in (timestamp - 180_000)..(timestamp + 180_000) &&
                ((upiRefNo != null && it.upiRefNo == upiRefNo) || it.rawMerchant == rawMerchant)
            }
        }

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

    @Test
    fun testGPayFollowedByHdfcSmsWithin3MinutesMergesCleanly() = runTest {
        val fakeTxDao = FakeTransactionDao()
        val fakeMemDao = FakeMemoryDao()
        val manager = DeduplicationManager(fakeTxDao, fakeMemDao)

        val t0 = 1700000000000L

        // 1. Google Pay alert arrives first at T0
        val gpayAlert = ParsedTransaction(
            amount = 540.0,
            type = TransactionType.DEBIT,
            rawMerchant = "Swiggy",
            upiVpa = null,
            upiRefNo = null,
            bankOrAppSource = "Google Pay",
            timestamp = t0
        )

        val firstResult = manager.process(gpayAlert)
        assertTrue(firstResult is DeduplicationResult.NewTransaction)
        val createdTx = (firstResult as DeduplicationResult.NewTransaction).transaction
        assertEquals("Swiggy", createdTx.displayMerchant)
        assertEquals(PulseCategory.FOOD_AND_DINING.id, createdTx.category)
        assertEquals(1, fakeTxDao.database.size)

        // 2. HDFC Bank SMS arrives 15 seconds later (T0 + 15,000ms) with UTR ref and VPA
        val bankSms = ParsedTransaction(
            amount = 540.0,
            type = TransactionType.DEBIT,
            rawMerchant = "Swiggy",
            upiVpa = "swiggy@icici",
            upiRefNo = "429182749102",
            bankOrAppSource = "HDFC Bank",
            timestamp = t0 + 15_000L
        )

        val secondResult = manager.process(bankSms)

        // MUST be merged rather than duplicated!
        assertTrue("Second transaction should be merged as duplicate", secondResult is DeduplicationResult.MergedAndEnriched)
        assertEquals(1, fakeTxDao.database.size) // Still exactly 1 transaction in DB

        val mergedTx = (secondResult as DeduplicationResult.MergedAndEnriched).updatedTransaction
        assertEquals("429182749102", mergedTx.upiRefNo)
        assertEquals("swiggy@icici", mergedTx.upiVpa)
        assertEquals("Google Pay + HDFC Bank", mergedTx.bankOrAppSource)
        assertEquals(PulseCategory.FOOD_AND_DINING.id, mergedTx.category)
    }

    @Test
    fun testTransactionsBeyond3MinutesAreNotMerged() = runTest {
        val fakeTxDao = FakeTransactionDao()
        val fakeMemDao = FakeMemoryDao()
        val manager = DeduplicationManager(fakeTxDao, fakeMemDao)

        val t0 = 1700000000000L

        val tx1 = ParsedTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            rawMerchant = "DMRC",
            upiRefNo = "429182749100",
            bankOrAppSource = "Paytm",
            timestamp = t0
        )
        manager.process(tx1)

        // Repeat recharge 5 minutes later (300 seconds > 180 seconds window)
        val tx2 = ParsedTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            rawMerchant = "DMRC",
            upiRefNo = "429182749101",
            bankOrAppSource = "Paytm",
            timestamp = t0 + 300_000L
        )
        val secondResult = manager.process(tx2)

        assertTrue(secondResult is DeduplicationResult.NewTransaction)
        assertEquals(2, fakeTxDao.database.size)
    }
}
