package com.example.pulseledger.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.dao.UpcomingMandateDao
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.TransactionType
import com.example.pulseledger.data.local.entity.UpcomingMandateEntity
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.local.model.DailySpend
import com.example.pulseledger.data.preference.IntelligencePreferences
import com.example.pulseledger.data.repository.ExpenseWaterfallRepository
import com.example.pulseledger.ui.model.PulseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ChartViewMode {
    CATEGORY_DONUT,
    SPEND_PULSE_7_DAY
}

data class DailyTransactionGroup(
    val dateLabel: String,
    val dayTotal: Double,
    val transactions: List<TransactionEntity>
)

data class DashboardUiState(
    val monthExpenses: Double = 0.0,
    val lastMonthExpenses: Double = 0.0,
    val spendVelocityPercent: Int = 0,
    val isSpendingHigher: Boolean = false,
    val pendingCount: Int = 0,
    val verificationPercentage: Int = 100,
    val pendingTransactions: List<TransactionEntity> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val dailySpends: List<DailySpend> = emptyList(),
    val groupedTransactions: List<DailyTransactionGroup> = emptyList(),
    val selectedCategoryFilter: PulseCategory? = null,
    val chartViewMode: ChartViewMode = ChartViewMode.CATEGORY_DONUT,
    val editingTransaction: TransactionEntity? = null,
    val isLoading: Boolean = true,
    // Step 8.5 Intelligence Properties
    val monthlyBudget: Double = 10000.0,
    val safeToSpendToday: Double = 0.0,
    val remainingDaysInMonth: Int = 1,
    val activeEventTag: String? = null,
    val selectedEventTagFilter: String? = null,
    val allEventTags: List<String> = emptyList(),
    val activeMandates: List<UpcomingMandateEntity> = emptyList()
)

class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val merchantMemoryDao: MerchantMemoryDao,
    private val upcomingMandateDao: UpcomingMandateDao,
    val waterfallRepository: ExpenseWaterfallRepository? = null,
    private val intelligencePrefs: IntelligencePreferences? = null,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val selectedCategoryFilter = MutableStateFlow<PulseCategory?>(null)
    private val selectedEventTagFilter = MutableStateFlow<String?>(null)
    private val chartViewMode = MutableStateFlow(ChartViewMode.CATEGORY_DONUT)
    private val editingTransaction = MutableStateFlow<TransactionEntity?>(null)

    // Current month and 7-day timestamp ranges
    private val startOfMonth: Long
    private val startOfLastMonth: Long
    private val startOf7DaysAgo: Long

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        startOfMonth = cal.timeInMillis

        cal.add(Calendar.MONTH, -1)
        startOfLastMonth = cal.timeInMillis

        val cal7 = Calendar.getInstance()
        cal7.add(Calendar.DAY_OF_YEAR, -6)
        cal7.set(Calendar.HOUR_OF_DAY, 0)
        cal7.set(Calendar.MINUTE, 0)
        cal7.set(Calendar.SECOND, 0)
        cal7.set(Calendar.MILLISECOND, 0)
        startOf7DaysAgo = cal7.timeInMillis
    }

    private data class DatabaseData(
        val allTx: List<TransactionEntity>,
        val pendingTx: List<TransactionEntity>,
        val rawCategorySpends: List<CategorySpend>,
        val rawDailySpends: List<DailySpend>,
        val activeMandates: List<UpcomingMandateEntity>,
        val allEventTags: List<String>
    )

    private data class UiPreferences(
        val categoryFilter: PulseCategory?,
        val eventTagFilter: String?,
        val chartMode: ChartViewMode,
        val editingTx: TransactionEntity?,
        val budget: Double,
        val activeTag: String?
    )

    private val dbDataFlow = combine(
        combine(
            transactionDao.getAllTransactionsFlow(),
            transactionDao.getPendingTransactionsFlow(),
            transactionDao.getCategorySpendTotalsFlow(startOfMonth, Long.MAX_VALUE)
        ) { allTx, pendingTx, rawCategorySpends ->
            Triple(allTx, pendingTx, rawCategorySpends)
        },
        combine(
            transactionDao.getDailySpendTotalsFlow(startOf7DaysAgo),
            upcomingMandateDao.getActiveMandatesFlow(),
            transactionDao.getAllEventTagsFlow()
        ) { rawDailySpends, mandates, eventTags ->
            Triple(rawDailySpends, mandates, eventTags)
        }
    ) { part1, part2 ->
        DatabaseData(
            allTx = part1.first,
            pendingTx = part1.second,
            rawCategorySpends = part1.third,
            rawDailySpends = part2.first,
            activeMandates = part2.second,
            allEventTags = part2.third
        )
    }

    private val uiPrefsFlow = combine(
        combine(
            selectedCategoryFilter,
            selectedEventTagFilter,
            chartViewMode
        ) { cat, tag, mode ->
            Triple(cat, tag, mode)
        },
        combine(
            editingTransaction,
            intelligencePrefs?.monthlyBudget ?: MutableStateFlow(10000.0),
            intelligencePrefs?.activeEventTag ?: MutableStateFlow<String?>(null)
        ) { editTx, budget, activeTag ->
            Triple(editTx, budget, activeTag)
        }
    ) { part1, part2 ->
        UiPreferences(
            categoryFilter = part1.first,
            eventTagFilter = part1.second,
            chartMode = part1.third,
            editingTx = part2.first,
            budget = part2.second,
            activeTag = part2.third
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        dbDataFlow,
        uiPrefsFlow
    ) { dbData, prefs ->
        val allTx = dbData.allTx
        val pendingTx = dbData.pendingTx
        val rawCategorySpends = dbData.rawCategorySpends
        val rawDailySpends = dbData.rawDailySpends
        val mandates = dbData.activeMandates
        val eventTags = dbData.allEventTags

        val categoryFilter = prefs.categoryFilter
        val eventFilter = prefs.eventTagFilter
        val chartMode = prefs.chartMode
        val editingTx = prefs.editingTx
        val monthlyBudget = prefs.budget
        val activeEventTag = prefs.activeTag

        // Step 8.5 Net Variable Spend Calculation:
        // Sums netAmount for DEBITs, subtracts netAmount for CREDIT where isRefund == true, excludes TRANSFER_NEUTRAL
        val currentMonthTotal = allTx.filter { it.timestamp >= startOfMonth }.sumOf { tx ->
            when {
                tx.type == TransactionType.DEBIT -> tx.netAmount
                tx.type == TransactionType.CREDIT && tx.isRefund -> -tx.netAmount
                else -> 0.0
            }
        }.coerceAtLeast(0.0)

        val lastMonthTotal = allTx.filter { it.timestamp in startOfLastMonth until startOfMonth }.sumOf { tx ->
            when {
                tx.type == TransactionType.DEBIT -> tx.netAmount
                tx.type == TransactionType.CREDIT && tx.isRefund -> -tx.netAmount
                else -> 0.0
            }
        }.coerceAtLeast(0.0)

        val velocity = if (lastMonthTotal > 0) {
            (((currentMonthTotal - lastMonthTotal) / lastMonthTotal) * 100).toInt()
        } else {
            0
        }

        val totalTransactions = allTx.size
        val verifiedCount = allTx.count { it.verificationState != VerificationState.PENDING_USER_TAP }
        val verificationPercent = if (totalTransactions > 0) {
            ((verifiedCount.toDouble() / totalTransactions) * 100).toInt()
        } else {
            100
        }

        // Safe-to-Spend Today calculation: (MonthlyBudget - NetVariableSpendMTD) / RemainingDaysInMonth
        val nowCal = Calendar.getInstance()
        val totalDaysInMonth = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (totalDaysInMonth - currentDay + 1).coerceAtLeast(1)
        val safeToSpendToday = (monthlyBudget - currentMonthTotal) / remainingDays

        // 48-Hour Renewal Radar: Active upcoming mandates due within 3 days (<= 72 hours from now)
        val nowMs = System.currentTimeMillis()
        val threeDaysAhead = nowMs + (3L * 86400000L)
        val filteredMandates = mandates.filter { it.dueDate <= threeDaysAhead }

        // Fill in missing days for 7-day Bar Chart so all 7 columns exist
        val filledDailySpends = generateFilled7Days(rawDailySpends)

        // Filter transactions if Event Tag Filter or Category Filter is active
        val filteredList = when {
            eventFilter != null -> allTx.filter { it.eventTag == eventFilter }
            categoryFilter != null -> allTx.filter { it.category == categoryFilter.id }
            else -> allTx
        }

        // Group transactions by Date Header (Today, Yesterday, Date)
        val grouped = groupTransactionsByDate(filteredList)

        DashboardUiState(
            monthExpenses = currentMonthTotal,
            lastMonthExpenses = lastMonthTotal,
            spendVelocityPercent = kotlin.math.abs(velocity),
            isSpendingHigher = velocity >= 0,
            pendingCount = pendingTx.size,
            verificationPercentage = verificationPercent,
            pendingTransactions = pendingTx,
            categorySpends = rawCategorySpends,
            dailySpends = filledDailySpends,
            groupedTransactions = grouped,
            selectedCategoryFilter = categoryFilter,
            chartViewMode = chartMode,
            editingTransaction = editingTx,
            isLoading = false,
            monthlyBudget = monthlyBudget,
            safeToSpendToday = safeToSpendToday,
            remainingDaysInMonth = remainingDays,
            activeEventTag = activeEventTag,
            selectedEventTagFilter = eventFilter,
            allEventTags = eventTags,
            activeMandates = filteredMandates
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setCategoryFilter(category: PulseCategory?) {
        selectedEventTagFilter.value = null
        selectedCategoryFilter.value = if (selectedCategoryFilter.value == category) null else category
    }

    fun setSelectedEventTagFilter(tag: String?) {
        selectedCategoryFilter.value = null
        selectedEventTagFilter.value = if (selectedEventTagFilter.value == tag) null else tag
    }

    fun setMonthlyBudget(budget: Double) {
        intelligencePrefs?.setMonthlyBudget(budget)
    }

    fun setActiveEventTag(tag: String?) {
        intelligencePrefs?.setActiveEventTag(tag)
    }

    fun clearActiveEventTag() {
        intelligencePrefs?.clearActiveEventTag()
    }

    fun setChartViewMode(mode: ChartViewMode) {
        chartViewMode.value = mode
    }

    fun openEditBottomSheet(transaction: TransactionEntity) {
        editingTransaction.value = transaction
    }

    fun closeEditBottomSheet() {
        editingTransaction.value = null
    }

    fun markMandatePaid(mandateId: String) {
        viewModelScope.launch(ioDispatcher) {
            upcomingMandateDao.markAsPaid(mandateId)
        }
    }

    fun dismissMandate(mandateId: String) {
        viewModelScope.launch(ioDispatcher) {
            upcomingMandateDao.dismissMandate(mandateId)
        }
    }

    fun linkSplitBill(creditTxId: String, parentDebitId: String, offsetAmount: Double) {
        viewModelScope.launch(ioDispatcher) {
            transactionDao.linkSplitBill(creditTxId, parentDebitId, offsetAmount)
        }
    }

    fun resolveTransaction(txId: String, lookupKey: String, category: String, customAlias: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            transactionDao.resolveAndRememberMerchant(
                txId = txId,
                lookupKey = lookupKey,
                category = category,
                customAlias = customAlias
            )
        }
    }

    fun updateMerchantRuleRetroactively(
        txId: String,
        lookupKey: String,
        newCategory: String,
        newAlias: String
    ) {
        viewModelScope.launch(ioDispatcher) {
            transactionDao.resolveAndRememberMerchant(
                txId = txId,
                lookupKey = lookupKey,
                category = newCategory,
                customAlias = newAlias.ifBlank { null }
            )
            withContext(Dispatchers.Main) {
                closeEditBottomSheet()
            }
        }
    }

    private fun generateFilled7Days(spends: List<DailySpend>): List<DailySpend> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val spendMap = spends.associateBy { it.dateString }
        val result = mutableListOf<DailySpend>()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val key = dateFormat.format(cal.time)
            val existing = spendMap[key]
            result.add(existing ?: DailySpend(dateString = key, totalAmount = 0.0))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun groupTransactionsByDate(transactions: List<TransactionEntity>): List<DailyTransactionGroup> {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val formatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val todayKey = formatKey.format(todayCal.time)
        val yesterdayKey = formatKey.format(yesterdayCal.time)

        val displayFormat = SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)

        return transactions
            .groupBy { formatKey.format(Date(it.timestamp)) }
            .map { (dateKey, txList) ->
                val label = when (dateKey) {
                    todayKey -> "Today"
                    yesterdayKey -> "Yesterday"
                    else -> txList.firstOrNull()?.let { displayFormat.format(Date(it.timestamp)) } ?: dateKey
                }
                val dayTotal = txList.sumOf { tx ->
                    when {
                        tx.type == TransactionType.DEBIT -> tx.netAmount
                        tx.type == TransactionType.CREDIT && tx.isRefund -> -tx.netAmount
                        else -> 0.0
                    }
                }
                DailyTransactionGroup(
                    dateLabel = label,
                    dayTotal = dayTotal,
                    transactions = txList.sortedByDescending { it.timestamp }
                )
            }
    }

    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(ioDispatcher) {
            transactionDao.insertTransaction(transaction)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = PulseDatabase.getInstance(appContext)
                    val prefs = IntelligencePreferences.getInstance(appContext)
                    val repo = ExpenseWaterfallRepository(
                        context = appContext,
                        transactionDao = db.transactionDao(),
                        merchantMemoryDao = db.merchantMemoryDao(),
                        upcomingMandateDao = db.upcomingMandateDao()
                    )
                    return DashboardViewModel(
                        transactionDao = db.transactionDao(),
                        merchantMemoryDao = db.merchantMemoryDao(),
                        upcomingMandateDao = db.upcomingMandateDao(),
                        waterfallRepository = repo,
                        intelligencePrefs = prefs
                    ) as T
                }
            }
    }
}
