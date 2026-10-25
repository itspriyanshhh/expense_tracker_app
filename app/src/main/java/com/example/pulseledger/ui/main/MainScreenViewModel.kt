package com.example.pulseledger.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.repository.PulseLedgerRepository
import com.example.pulseledger.data.repository.RawIngestedTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val pendingTransactions: List<TransactionEntity> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val categorySpends: List<CategorySpend> = emptyList(),
    val isSeeded: Boolean = false
)

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val state: DashboardUiState) : MainScreenUiState
}

class MainScreenViewModel(
    private val repository: PulseLedgerRepository
) : ViewModel() {

    constructor(application: Application) : this(
        PulseLedgerRepository(
            PulseDatabase.getInstance(application).transactionDao(),
            PulseDatabase.getInstance(application).merchantMemoryDao()
        )
    )

    val uiState: StateFlow<MainScreenUiState> = combine(
        repository.allTransactions,
        repository.pendingTransactions
    ) { allTx, pendingTx ->
        val monthExpenses = allTx.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val monthIncome = allTx.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

        val categorySpends = allTx
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .map { (cat, txs) -> CategorySpend(cat, txs.sumOf { it.amount }, txs.size) }
            .sortedByDescending { it.totalAmount }

        MainScreenUiState.Success(
            DashboardUiState(
                transactions = allTx,
                pendingTransactions = pendingTx,
                totalExpenses = monthExpenses,
                totalIncome = monthIncome,
                categorySpends = categorySpends,
                isSeeded = allTx.isNotEmpty()
            )
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainScreenUiState.Loading
    )

    init {
        seedInitialDemoDataIfEmpty()
    }

    private fun seedInitialDemoDataIfEmpty() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sampleIngests = listOf(
                RawIngestedTransaction("tx_swiggy", 540.0, TransactionType.DEBIT, "SWIGGY GOURMET", "swiggy@icici", "429182749102", "GPay", now - 12 * 60 * 1000),
                RawIngestedTransaction("tx_zepto", 380.0, TransactionType.DEBIT, "ZEPTO QUICK", "zepto@axis", "429182749103", "PhonePe", now - 2 * 3600 * 1000),
                RawIngestedTransaction("tx_dmrc", 200.0, TransactionType.DEBIT, "DMRC DELHI METRO", "dmrc@paytm", "429182749104", "Paytm", now - 5 * 3600 * 1000),
                RawIngestedTransaction("tx_p2p_suresh", 1250.0, TransactionType.DEBIT, "Suresh Yadav", "suresh.yadav@upi", "429182749105", "GPay", now - 6 * 3600 * 1000),
                RawIngestedTransaction("tx_salary", 85000.0, TransactionType.CREDIT, "Client Settlement (TechCorp)", null, "429182749106", "HDFC Bank", now - 24 * 3600 * 1000),
                RawIngestedTransaction("tx_amazon", 2499.0, TransactionType.DEBIT, "AMAZON PAYMENTS", "amazonpay@apl", "429182749107", "Amazon Pay", now - 28 * 3600 * 1000)
            )

            for (sample in sampleIngests) {
                repository.ingestTransaction(sample)
            }
        }
    }

    fun resolveTransaction(txId: String, lookupKey: String, category: String, customAlias: String? = null) {
        viewModelScope.launch {
            repository.resolveAndRememberMerchant(txId, lookupKey, category, customAlias)
        }
    }

    companion object {
        fun provideFactory(context: android.content.Context): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = PulseDatabase.getInstance(context.applicationContext)
                    val repo = PulseLedgerRepository(db.transactionDao(), db.merchantMemoryDao())
                    return MainScreenViewModel(repo) as T
                }
            }
    }
}
