package com.example.pulseledger.ui.screens.dashboard

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardViewModelTest {

    private class FakeTransactionDao : TransactionDao {
        val allTxFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val pendingTxFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val catSpendFlow = MutableStateFlow<List<CategorySpend>>(emptyList())
        val dailySpendFlow = MutableStateFlow<List<DailySpend>>(emptyList())

        var lastResolvedTxId: String? = null
        var lastResolvedLookupKey: String? = null
        var lastResolvedCategory: String? = null
        var lastResolvedAlias: String? = null

        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun getTransactionById(id: String): TransactionEntity? = null
        override suspend fun deleteTransaction(id: String): Int = 1
        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = allTxFlow
        override fun getTransactionsByDateRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>> = allTxFlow
        override fun getTransactionsByCategoryFlow(category: String): Flow<List<TransactionEntity>> = allTxFlow
        override fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>> = pendingTxFlow
        override fun getPendingCountFlow(): Flow<Int> = MutableStateFlow(pendingTxFlow.value.size)
        override fun getCategorySpendTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpend>> = catSpendFlow
        override fun getDailySpendTotalsFlow(startTimestamp: Long): Flow<List<DailySpend>> = dailySpendFlow
        override suspend fun findSlidingDuplicate(amount: Double, upiRefNo: String?, rawMerchant: String, timestamp: Long): TransactionEntity? = null
        override suspend fun findWindowCandidates(amount: Double, type: TransactionType, timestamp: Long): List<TransactionEntity> = emptyList()

        override suspend fun updateTransactionResolution(txId: String, category: String, customAlias: String?): Int = 1
        override suspend fun retroactivelyUpdatePendingTransactions(lookupKey: String, category: String, customAlias: String?): Int = 1
        override suspend fun upsertMerchantMemory(lookupKey: String, category: String, customAlias: String?, timestamp: Long): Long = 1

        override suspend fun resolveAndRememberMerchant(
            txId: String,
            lookupKey: String,
            category: String,
            customAlias: String?
        ) {
            lastResolvedTxId = txId
            lastResolvedLookupKey = lookupKey
            lastResolvedCategory = category
            lastResolvedAlias = customAlias
        }
    }

    private class FakeMemoryDao : MerchantMemoryDao {
        override suspend fun getMerchantMemory(lookupKey: String): MerchantMemoryEntity? = null
        override fun getAllMerchantMemoryFlow(): Flow<List<MerchantMemoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun upsert(entity: MerchantMemoryEntity) {}
        override suspend fun update(entity: MerchantMemoryEntity) {}
        override suspend fun delete(lookupKey: String): Int = 1
        override suspend fun getMemoryCount(): Int = 0
    }

    @Test
    fun testDashboardViewModelInitializationAndVelocity() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()

        val now = System.currentTimeMillis()
        val sampleTxs = listOf(
            TransactionEntity(
                id = "tx1",
                amount = 1500.0,
                type = TransactionType.DEBIT,
                rawMerchant = "ZEPTO",
                displayMerchant = "Zepto",
                bankOrAppSource = "PhonePe",
                category = PulseCategory.GROCERIES.id,
                timestamp = now,
                verificationState = VerificationState.VERIFIED_RULE
            ),
            TransactionEntity(
                id = "tx2",
                amount = 60.0,
                type = TransactionType.DEBIT,
                rawMerchant = "RAMESH KUMAR",
                displayMerchant = "RAMESH KUMAR",
                bankOrAppSource = "GPay",
                category = PulseCategory.UNCATEGORIZED_P2P.id,
                timestamp = now,
                verificationState = VerificationState.PENDING_USER_TAP
            )
        )
        txDao.allTxFlow.value = sampleTxs
        txDao.pendingTxFlow.value = listOf(sampleTxs[1])

        val viewModel = DashboardViewModel(txDao, memDao)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1560.0, state.monthExpenses, 0.01)
        assertEquals(1, state.pendingCount)
        assertEquals(1, state.pendingTransactions.size)
    }

    @Test
    fun testCategoryFilterSelection() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()
        val viewModel = DashboardViewModel(txDao, memDao)

        viewModel.setCategoryFilter(PulseCategory.FOOD_AND_DINING)
        var state = viewModel.uiState.first { it.selectedCategoryFilter == PulseCategory.FOOD_AND_DINING }
        assertEquals(PulseCategory.FOOD_AND_DINING, state.selectedCategoryFilter)

        // Tapping same category toggles filter off
        viewModel.setCategoryFilter(PulseCategory.FOOD_AND_DINING)
        state = viewModel.uiState.first { it.selectedCategoryFilter == null && !it.isLoading }
        assertNull(state.selectedCategoryFilter)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testResolveTransactionCallsDao() = runTest {
        val txDao = FakeTransactionDao()
        val memDao = FakeMemoryDao()
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)
        val viewModel = DashboardViewModel(txDao, memDao, waterfallRepository = null, ioDispatcher = testDispatcher)

        viewModel.resolveTransaction("tx_123", "ramesh@upi", PulseCategory.TRANSIT_AND_TRAVEL.id, "Local Auto")

        assertEquals("tx_123", txDao.lastResolvedTxId)
        assertEquals("ramesh@upi", txDao.lastResolvedLookupKey)
        assertEquals(PulseCategory.TRANSIT_AND_TRAVEL.id, txDao.lastResolvedCategory)
        assertEquals("Local Auto", txDao.lastResolvedAlias)
    }
}
