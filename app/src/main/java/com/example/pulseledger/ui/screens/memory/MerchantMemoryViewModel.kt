package com.example.pulseledger.ui.screens.memory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.ui.model.PulseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LearnedMerchantUiModel(
    val entity: MerchantMemoryEntity,
    val category: PulseCategory,
    val totalSpent: Double,
    val transactionCount: Int
)

data class MerchantMemoryUiState(
    val merchants: List<LearnedMerchantUiModel> = emptyList(),
    val searchQuery: String = "",
    val totalLearnedCount: Int = 0,
    val totalVolumeSpent: Double = 0.0,
    val isPreMappingSheetOpen: Boolean = false,
    val editingMerchant: MerchantMemoryEntity? = null,
    val isLoading: Boolean = true
)

class MerchantMemoryViewModel(
    private val merchantMemoryDao: MerchantMemoryDao,
    private val transactionDao: TransactionDao,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isPreMappingSheetOpen = MutableStateFlow(false)
    private val editingMerchant = MutableStateFlow<MerchantMemoryEntity?>(null)

    val uiState: StateFlow<MerchantMemoryUiState> = combine(
        merchantMemoryDao.getAllMerchantMemoryFlow(),
        transactionDao.getAllTransactionsFlow(),
        searchQuery,
        isPreMappingSheetOpen,
        editingMerchant
    ) { memories, allTx, query, preMappingOpen, editing ->
        val debits = allTx.filter { it.type == TransactionType.DEBIT }

        val spendByPayee = debits.groupBy { tx ->
            val vpa = tx.upiVpa?.trim()?.lowercase()
            if (!vpa.isNullOrBlank()) vpa else tx.rawMerchant.trim().lowercase()
        }

        val allItems = memories.map { mem ->
            val key = mem.lookupKey.trim().lowercase()
            val matchedTxs = debits.filter { tx ->
                tx.upiVpa?.trim()?.equals(key, ignoreCase = true) == true ||
                tx.rawMerchant.trim().equals(key, ignoreCase = true) ||
                (mem.customAlias != null && tx.displayMerchant.equals(mem.customAlias, ignoreCase = true))
            }
            val total = matchedTxs.sumOf { it.amount }
            val count = maxOf(mem.transactionCount, matchedTxs.size)
            val cat = PulseCategory.fromString(mem.category)

            LearnedMerchantUiModel(
                entity = mem,
                category = cat,
                totalSpent = total,
                transactionCount = count
            )
        }

        val filteredItems = if (query.isBlank()) {
            allItems
        } else {
            val q = query.trim().lowercase()
            allItems.filter { item ->
                item.entity.lookupKey.lowercase().contains(q) ||
                item.entity.customAlias?.lowercase()?.contains(q) == true ||
                item.category.displayName.lowercase().contains(q)
            }
        }

        MerchantMemoryUiState(
            merchants = filteredItems,
            searchQuery = query,
            totalLearnedCount = memories.size,
            totalVolumeSpent = allItems.sumOf { it.totalSpent },
            isPreMappingSheetOpen = preMappingOpen,
            editingMerchant = editing,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MerchantMemoryUiState()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun openPreMapping() {
        isPreMappingSheetOpen.value = true
    }

    fun closePreMapping() {
        isPreMappingSheetOpen.value = false
    }

    fun openEdit(entity: MerchantMemoryEntity) {
        editingMerchant.value = entity
    }

    fun closeEdit() {
        editingMerchant.value = null
    }

    fun savePreMapping(lookupKey: String, customAlias: String?, category: String) {
        val cleanKey = lookupKey.trim().lowercase()
        val cleanAlias = customAlias?.trim()?.ifBlank { null }
        viewModelScope.launch(ioDispatcher) {
            merchantMemoryDao.rememberMerchant(
                lookupKey = cleanKey,
                category = category,
                customAlias = cleanAlias
            )
            // Retroactively link any existing transactions
            transactionDao.retroactivelyUpdatePendingTransactions(cleanKey, category, cleanAlias)
            closePreMapping()
        }
    }

    fun updateMerchantRule(lookupKey: String, newAlias: String?, newCategory: String) {
        val cleanKey = lookupKey.trim().lowercase()
        val cleanAlias = newAlias?.trim()?.ifBlank { null }
        viewModelScope.launch(ioDispatcher) {
            merchantMemoryDao.rememberMerchant(
                lookupKey = cleanKey,
                category = newCategory,
                customAlias = cleanAlias
            )
            transactionDao.retroactivelyUpdatePendingTransactions(cleanKey, newCategory, cleanAlias)
            closeEdit()
        }
    }

    fun deleteMerchantRule(lookupKey: String) {
        viewModelScope.launch(ioDispatcher) {
            merchantMemoryDao.delete(lookupKey.trim().lowercase())
            closeEdit()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = PulseDatabase.getInstance(context.applicationContext)
                    return MerchantMemoryViewModel(db.merchantMemoryDao(), db.transactionDao()) as T
                }
            }
    }
}
