package com.example.pulseledger.ui.screens.memory

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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantMemoryViewModelTest {

    private class FakeMerchantMemoryDao : MerchantMemoryDao {
        val memoryFlow = MutableStateFlow<List<MerchantMemoryEntity>>(emptyList())
        val rememberedList = mutableListOf<Triple<String, String, String?>>()
        val deletedList = mutableListOf<String>()

        override suspend fun getMerchantMemory(lookupKey: String): MerchantMemoryEntity? =
            memoryFlow.value.find { it.lookupKey == lookupKey }

        override fun getAllMerchantMemoryFlow(): Flow<List<MerchantMemoryEntity>> = memoryFlow

        override suspend fun upsert(entity: MerchantMemoryEntity) {}
        override suspend fun update(entity: MerchantMemoryEntity) {}

        override suspend fun rememberMerchant(
            lookupKey: String,
            category: String,
            customAlias: String?,
            timestamp: Long
        ) {
            rememberedList.add(Triple(lookupKey, category, customAlias))
            val existing = memoryFlow.value.toMutableList()
            existing.removeAll { it.lookupKey == lookupKey }
            existing.add(
                MerchantMemoryEntity(
                    lookupKey = lookupKey,
                    category = category,
                    customAlias = customAlias,
                    transactionCount = 1,
                    lastUpdated = timestamp
                )
            )
            memoryFlow.value = existing
        }

        override suspend fun delete(lookupKey: String): Int {
            deletedList.add(lookupKey)
            memoryFlow.value = memoryFlow.value.filter { it.lookupKey != lookupKey }
            return 1
        }

        override suspend fun getMemoryCount(): Int = memoryFlow.value.size
    }

    private class FakeTransactionDao : TransactionDao {
        val allTxFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val retroUpdated = mutableListOf<Triple<String, String, String?>>()

        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun getTransactionById(id: String): TransactionEntity? = null
        override suspend fun deleteTransaction(id: String): Int = 1
        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = allTxFlow
        override fun getTransactionsByDateRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>> = allTxFlow
        override fun getTransactionsByCategoryFlow(category: String): Flow<List<TransactionEntity>> = allTxFlow
        override fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>> = allTxFlow
        override fun getPendingCountFlow(): Flow<Int> = MutableStateFlow(0)
        override fun getCategorySpendTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpend>> = MutableStateFlow(emptyList())
        override fun getDailySpendTotalsFlow(startTimestamp: Long): Flow<List<DailySpend>> = MutableStateFlow(emptyList())
        override suspend fun findSlidingDuplicate(amount: Double, upiRefNo: String?, rawMerchant: String, timestamp: Long): TransactionEntity? = null
        override suspend fun findWindowCandidates(amount: Double, type: TransactionType, timestamp: Long): List<TransactionEntity> = emptyList()
        override suspend fun updateTransactionResolution(txId: String, category: String, customAlias: String?): Int = 1

        override suspend fun retroactivelyUpdatePendingTransactions(lookupKey: String, category: String, customAlias: String?): Int {
            retroUpdated.add(Triple(lookupKey, category, customAlias))
            return 1
        }

        override suspend fun upsertMerchantMemory(lookupKey: String, category: String, customAlias: String?, timestamp: Long): Long = 1
        override suspend fun resolveAndRememberMerchant(txId: String, lookupKey: String, category: String, customAlias: String?) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testInitialStateCombinesFlowsAndComputesTotals() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val memoryDao = FakeMerchantMemoryDao()
        val txDao = FakeTransactionDao()

        val sampleMemory = listOf(
            MerchantMemoryEntity(
                lookupKey = "suresh@ybl",
                category = PulseCategory.TRANSIT_AND_TRAVEL.id,
                customAlias = "Suresh Auto",
                transactionCount = 3,
                lastUpdated = 1000L
            ),
            MerchantMemoryEntity(
                lookupKey = "sharma.grocery@upi",
                category = PulseCategory.GROCERIES.id,
                customAlias = "Sharma Ji Kirana",
                transactionCount = 2,
                lastUpdated = 2000L
            )
        )
        memoryDao.memoryFlow.value = sampleMemory

        val sampleTx = listOf(
            TransactionEntity(
                id = "tx1",
                amount = 50.0,
                type = TransactionType.DEBIT,
                rawMerchant = "Suresh Yadav",
                displayMerchant = "Suresh Auto",
                upiVpa = "suresh@ybl",
                upiRefNo = null,
                bankOrAppSource = "PhonePe",
                category = PulseCategory.TRANSIT_AND_TRAVEL.id,
                timestamp = 1000L,
                verificationState = VerificationState.VERIFIED_MEMORY
            ),
            TransactionEntity(
                id = "tx2",
                amount = 40.0,
                type = TransactionType.DEBIT,
                rawMerchant = "Suresh Yadav",
                displayMerchant = "Suresh Auto",
                upiVpa = "suresh@ybl",
                upiRefNo = null,
                bankOrAppSource = "PhonePe",
                category = PulseCategory.TRANSIT_AND_TRAVEL.id,
                timestamp = 1100L,
                verificationState = VerificationState.VERIFIED_MEMORY
            ),
            TransactionEntity(
                id = "tx3",
                amount = 500.0,
                type = TransactionType.DEBIT,
                rawMerchant = "Sharma General Store",
                displayMerchant = "Sharma Ji Kirana",
                upiVpa = "sharma.grocery@upi",
                upiRefNo = null,
                bankOrAppSource = "GPay",
                category = PulseCategory.GROCERIES.id,
                timestamp = 2000L,
                verificationState = VerificationState.VERIFIED_MEMORY
            )
        )
        txDao.allTxFlow.value = sampleTx

        val viewModel = MerchantMemoryViewModel(memoryDao, txDao, ioDispatcher = testDispatcher)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(2, state.totalLearnedCount)
        assertEquals(590.0, state.totalVolumeSpent, 0.01)
        assertEquals(2, state.merchants.size)

        val sureshItem = state.merchants.first { it.entity.lookupKey == "suresh@ybl" }
        assertEquals("Suresh Auto", sureshItem.entity.customAlias)
        assertEquals(90.0, sureshItem.totalSpent, 0.01)
        assertEquals(3, sureshItem.transactionCount)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testSearchFilteringByAliasVpaAndCategory() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val memoryDao = FakeMerchantMemoryDao()
        val txDao = FakeTransactionDao()

        memoryDao.memoryFlow.value = listOf(
            MerchantMemoryEntity("suresh@ybl", PulseCategory.TRANSIT_AND_TRAVEL.id, "Suresh Auto", 1, 100L),
            MerchantMemoryEntity("dmrc@sbi", PulseCategory.TRANSIT_AND_TRAVEL.id, "Delhi Metro", 5, 200L),
            MerchantMemoryEntity("zepto@icici", PulseCategory.GROCERIES.id, "Zepto Quick", 2, 300L)
        )

        val viewModel = MerchantMemoryViewModel(memoryDao, txDao, ioDispatcher = testDispatcher)

        // 1. Search by VPA query
        viewModel.setSearchQuery("dmrc")
        val vpaFiltered = viewModel.uiState.first { !it.isLoading && it.searchQuery == "dmrc" }
        assertEquals(1, vpaFiltered.merchants.size)
        assertEquals("dmrc@sbi", vpaFiltered.merchants.first().entity.lookupKey)

        // 2. Search by custom alias query
        viewModel.setSearchQuery("Auto")
        val aliasFiltered = viewModel.uiState.first { !it.isLoading && it.searchQuery == "Auto" }
        assertEquals(1, aliasFiltered.merchants.size)
        assertEquals("Suresh Auto", aliasFiltered.merchants.first().entity.customAlias)

        // 3. Search by category name query
        viewModel.setSearchQuery("Groceries")
        val catFiltered = viewModel.uiState.first { !it.isLoading && it.searchQuery == "Groceries" }
        assertEquals(1, catFiltered.merchants.size)
        assertEquals("Zepto Quick", catFiltered.merchants.first().entity.customAlias)

        // 4. Clear search
        viewModel.setSearchQuery("")
        val cleared = viewModel.uiState.first { !it.isLoading && it.searchQuery.isEmpty() }
        assertEquals(3, cleared.merchants.size)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testPreMappingSavesToDaoAndRetroactivelyUpdates() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val memoryDao = FakeMerchantMemoryDao()
        val txDao = FakeTransactionDao()

        val viewModel = MerchantMemoryViewModel(memoryDao, txDao, ioDispatcher = testDispatcher)

        viewModel.openPreMapping()
        assertTrue(viewModel.uiState.first { it.isPreMappingSheetOpen }.isPreMappingSheetOpen)

        viewModel.savePreMapping(
            lookupKey = "chaiwala@paytm",
            customAlias = "Nandu Chai",
            category = PulseCategory.FOOD_AND_DINING.id
        )

        val stateAfterSave = viewModel.uiState.first { !it.isPreMappingSheetOpen && !it.isLoading }
        assertFalse(stateAfterSave.isPreMappingSheetOpen)
        assertEquals(1, memoryDao.rememberedList.size)
        assertEquals("chaiwala@paytm", memoryDao.rememberedList.first().first)
        assertEquals(PulseCategory.FOOD_AND_DINING.id, memoryDao.rememberedList.first().second)
        assertEquals("Nandu Chai", memoryDao.rememberedList.first().third)

        assertEquals(1, txDao.retroUpdated.size)
        assertEquals("chaiwala@paytm", txDao.retroUpdated.first().first)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testDeleteMerchantRuleCallsDao() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val memoryDao = FakeMerchantMemoryDao()
        val txDao = FakeTransactionDao()

        val item = MerchantMemoryEntity("vendor@upi", PulseCategory.SHOPPING.id, "Local Store", 1, 100L)
        memoryDao.memoryFlow.value = listOf(item)

        val viewModel = MerchantMemoryViewModel(memoryDao, txDao, ioDispatcher = testDispatcher)
        viewModel.openEdit(item)
        val editState = viewModel.uiState.first { it.editingMerchant != null }
        assertNotNull(editState.editingMerchant)

        viewModel.deleteMerchantRule("vendor@upi")
        val deletedState = viewModel.uiState.first { it.editingMerchant == null && !it.isLoading }
        assertNull(deletedState.editingMerchant)
        assertEquals(1, memoryDao.deletedList.size)
        assertEquals("vendor@upi", memoryDao.deletedList.first())
    }
}
