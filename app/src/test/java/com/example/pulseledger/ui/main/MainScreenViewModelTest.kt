package com.example.pulseledger.ui.main

import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.local.model.DailySpend
import com.example.pulseledger.data.repository.PulseLedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

    private val fakeTransactionDao = object : TransactionDao {
        val listFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val pendingFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val countFlow = MutableStateFlow(0)

        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun getTransactionById(id: String): TransactionEntity? = null
        override suspend fun deleteTransaction(id: String): Int = 1
        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = listFlow
        override fun getTransactionsByDateRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>> = listFlow
        override fun getTransactionsByCategoryFlow(category: String): Flow<List<TransactionEntity>> = listFlow
        override fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>> = pendingFlow
        override fun getPendingCountFlow(): Flow<Int> = countFlow
        override fun getCategorySpendTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpend>> = MutableStateFlow(emptyList())
        override fun getDailySpendTotalsFlow(startTimestamp: Long): Flow<List<DailySpend>> = MutableStateFlow(emptyList())
        override suspend fun findSlidingDuplicate(amount: Double, upiRefNo: String?, rawMerchant: String, timestamp: Long): TransactionEntity? = null
        override suspend fun findWindowCandidates(amount: Double, type: com.example.pulseledger.data.local.entity.TransactionType, timestamp: Long): List<TransactionEntity> = emptyList()
        override suspend fun updateTransactionResolution(txId: String, category: String, customAlias: String?): Int = 1
        override suspend fun retroactivelyUpdatePendingTransactions(lookupKey: String, category: String, customAlias: String?): Int = 0
        override suspend fun upsertMerchantMemory(lookupKey: String, category: String, customAlias: String?, timestamp: Long): Long = 1
    }

    private val fakeMemoryDao = object : MerchantMemoryDao {
        override suspend fun getMerchantMemory(lookupKey: String): MerchantMemoryEntity? = null
        override fun getAllMerchantMemoryFlow(): Flow<List<MerchantMemoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun upsert(entity: MerchantMemoryEntity) {}
        override suspend fun update(entity: MerchantMemoryEntity) {}
        override suspend fun delete(lookupKey: String): Int = 1
        override suspend fun getMemoryCount(): Int = 0
    }

    @Test
    fun uiState_initialStateLoadsSuccessfully() = runTest {
        val repo = PulseLedgerRepository(fakeTransactionDao, fakeMemoryDao)
        val viewModel = MainScreenViewModel(repo)
        val state = viewModel.uiState.first()
        assertNotNull(state)
        assertTrue(state is MainScreenUiState.Success || state is MainScreenUiState.Loading)
    }
}
