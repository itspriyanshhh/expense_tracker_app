package com.example.pulseledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.model.CategorySpend
import com.example.pulseledger.data.local.model.DailySpend
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String): Int

    // Flow 1: All transactions ordered by timestamp DESC
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    // Flow 2: Transactions within a date range (e.g. active month)
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByDateRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>>

    // Flow 3: Transactions filtered by category
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategoryFlow(category: String): Flow<List<TransactionEntity>>

    // Flow 4: Filtered by PENDING_USER_TAP state
    @Query("SELECT * FROM transactions WHERE verificationState = 'PENDING_USER_TAP' ORDER BY timestamp DESC")
    fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE verificationState = 'PENDING_USER_TAP'")
    fun getPendingCountFlow(): Flow<Int>

    // Flow 5: Category-wise spend totals for the active month (for Donut Chart)
    // Sums netAmount and automatically subtracts CREDIT rows where isRefund == true
    @Query("""
        SELECT category,
               SUM(CASE WHEN type = 'DEBIT' THEN netAmount WHEN type = 'CREDIT' AND isRefund = 1 THEN -netAmount ELSE 0.0 END) AS totalAmount,
               COUNT(CASE WHEN type = 'DEBIT' THEN 1 END) AS transactionCount
        FROM transactions 
        WHERE (type = 'DEBIT' OR (type = 'CREDIT' AND isRefund = 1))
          AND timestamp >= :startTimestamp AND timestamp < :endTimestamp 
        GROUP BY category 
        HAVING totalAmount > 0
        ORDER BY totalAmount DESC
    """)
    fun getCategorySpendTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpend>>

    // Flow 6: Daily spend totals for the last 7 days (for Bar Chart)
    @Query("""
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dateString, 
               SUM(CASE WHEN type = 'DEBIT' THEN netAmount WHEN type = 'CREDIT' AND isRefund = 1 THEN -netAmount ELSE 0.0 END) AS totalAmount 
        FROM transactions 
        WHERE (type = 'DEBIT' OR (type = 'CREDIT' AND isRefund = 1))
          AND timestamp >= :startTimestamp 
        GROUP BY dateString 
        ORDER BY dateString ASC
    """)
    fun getDailySpendTotalsFlow(startTimestamp: Long): Flow<List<DailySpend>>

    // Net Variable Monthly Spend query
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN netAmount WHEN type = 'CREDIT' AND isRefund = 1 THEN -netAmount ELSE 0.0 END), 0.0)
        FROM transactions
        WHERE (type = 'DEBIT' OR (type = 'CREDIT' AND isRefund = 1))
          AND timestamp >= :startTimestamp AND timestamp < :endTimestamp
    """)
    fun getNetSpendTotalFlow(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    // 3-Minute Sliding Deduplication Query
    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount 
          AND timestamp BETWEEN (:timestamp - 180000) AND (:timestamp + 180000)
          AND (:upiRefNo IS NOT NULL AND upiRefNo = :upiRefNo OR rawMerchant = :rawMerchant)
        LIMIT 1
    """)
    suspend fun findSlidingDuplicate(
        amount: Double,
        upiRefNo: String?,
        rawMerchant: String,
        timestamp: Long
    ): TransactionEntity?

    // 3-Minute Sliding Deduplication Window candidates matching amount and type
    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount 
          AND type = :type
          AND timestamp BETWEEN (:timestamp - 180000) AND (:timestamp + 180000)
        ORDER BY timestamp DESC
    """)
    suspend fun findWindowCandidates(
        amount: Double,
        type: com.example.pulseledger.data.local.entity.TransactionType,
        timestamp: Long
    ): List<TransactionEntity>

    // Step 8.5 Commercial Refund Reconciler Helper (lookup DEBIT from same merchant in past 14 days)
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'DEBIT'
          AND timestamp BETWEEN (:currentTimestamp - 1209600000) AND :currentTimestamp
          AND (
              LOWER(rawMerchant) LIKE '%' || LOWER(:merchantKeyword) || '%'
              OR LOWER(displayMerchant) LIKE '%' || LOWER(:merchantKeyword) || '%'
              OR (:upiVpaPrefix != '' AND LOWER(COALESCE(upiVpa, '')) LIKE '%' || LOWER(:upiVpaPrefix) || '%')
          )
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun findRecentDebitForMerchant(
        merchantKeyword: String,
        upiVpaPrefix: String,
        currentTimestamp: Long
    ): TransactionEntity?

    // Step 8.5 P2P Split Bill Linker Helper (lookup DEBIT >= creditAmount in past 24 hours)
    @Query("""
        SELECT * FROM transactions
        WHERE type = 'DEBIT'
          AND amount >= :creditAmount
          AND timestamp BETWEEN (:currentTimestamp - 86400000) AND :currentTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun findRecentEligibleDebitForSplit(
        creditAmount: Double,
        currentTimestamp: Long
    ): TransactionEntity?

    @Query("UPDATE transactions SET netAmount = MAX(0.0, netAmount - :offsetAmount) WHERE id = :parentDebitId")
    suspend fun reduceNetAmount(parentDebitId: String, offsetAmount: Double): Int

    @Query("UPDATE transactions SET parentTxId = :parentDebitId, isRefund = :isRefund, category = :category WHERE id = :creditTxId")
    suspend fun linkParentTransaction(creditTxId: String, parentDebitId: String, isRefund: Boolean, category: String): Int

    @Transaction
    suspend fun linkSplitBill(creditTxId: String, parentDebitId: String, offsetAmount: Double) {
        val parent = getTransactionById(parentDebitId) ?: return
        reduceNetAmount(parentDebitId, offsetAmount)
        linkParentTransaction(creditTxId, parentDebitId, isRefund = false, category = parent.category)
    }

    // Step 8.5 Event Mode Queries
    @Query("""
        SELECT * FROM transactions
        WHERE eventTag = :eventTag
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByEventTagFlow(eventTag: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT DISTINCT eventTag FROM transactions
        WHERE eventTag IS NOT NULL AND eventTag != ''
        ORDER BY timestamp DESC
    """)
    fun getAllEventTagsFlow(): Flow<List<String>>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN netAmount WHEN type = 'CREDIT' AND isRefund = 1 THEN -netAmount ELSE 0.0 END), 0.0)
        FROM transactions
        WHERE eventTag = :eventTag
    """)
    fun getEventSpendTotalFlow(eventTag: String): Flow<Double>

    @Query("""
        UPDATE transactions 
        SET category = :category, 
            verificationState = 'VERIFIED_USER', 
            displayMerchant = COALESCE(:customAlias, displayMerchant) 
        WHERE id = :txId
    """)
    suspend fun updateTransactionResolution(txId: String, category: String, customAlias: String?): Int

    @Query("""
        UPDATE transactions 
        SET category = :category, 
            verificationState = 'VERIFIED_USER', 
            displayMerchant = COALESCE(:customAlias, displayMerchant) 
        WHERE verificationState = 'PENDING_USER_TAP' 
          AND (
            (:lookupKey != '' AND LOWER(COALESCE(upiVpa, '')) = LOWER(:lookupKey)) 
            OR LOWER(rawMerchant) = LOWER(:lookupKey)
          )
    """)
    suspend fun retroactivelyUpdatePendingTransactions(lookupKey: String, category: String, customAlias: String?): Int

    @Query("""
        INSERT INTO merchant_memory (lookupKey, category, customAlias, transactionCount, lastUpdated) 
        VALUES (:lookupKey, :category, :customAlias, 1, :timestamp)
        ON CONFLICT(lookupKey) DO UPDATE SET 
            category = :category, 
            customAlias = COALESCE(:customAlias, merchant_memory.customAlias), 
            transactionCount = merchant_memory.transactionCount + 1, 
            lastUpdated = :timestamp
    """)
    suspend fun upsertMerchantMemory(lookupKey: String, category: String, customAlias: String?, timestamp: Long): Long

    /**
     * Atomically resolves a transaction, commits the vendor rule to Tier 1 merchant_memory,
     * and retroactively clears any sibling pending transactions from the same payee.
     */
    @Transaction
    suspend fun resolveAndRememberMerchant(
        txId: String,
        lookupKey: String,
        category: String,
        customAlias: String? = null
    ) {
        val now = System.currentTimeMillis()
        val normalizedKey = lookupKey.trim().lowercase()

        // 1. Update the tapped transaction
        updateTransactionResolution(txId, category, customAlias)

        // 2. Persist in Tier 1 Merchant Memory
        upsertMerchantMemory(normalizedKey, category, customAlias, now)

        // 3. Retroactively update other pending transactions from this payee
        retroactivelyUpdatePendingTransactions(normalizedKey, category, customAlias)
    }
}
