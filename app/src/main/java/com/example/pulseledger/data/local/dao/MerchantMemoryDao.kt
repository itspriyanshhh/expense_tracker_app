package com.example.pulseledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantMemoryDao {

    @Query("SELECT * FROM merchant_memory WHERE lookupKey = :lookupKey LIMIT 1")
    suspend fun getMerchantMemory(lookupKey: String): MerchantMemoryEntity?

    @Query("SELECT * FROM merchant_memory ORDER BY transactionCount DESC, lastUpdated DESC")
    fun getAllMerchantMemoryFlow(): Flow<List<MerchantMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MerchantMemoryEntity)

    @Update
    suspend fun update(entity: MerchantMemoryEntity)

    @Query("DELETE FROM merchant_memory WHERE lookupKey = :lookupKey")
    suspend fun delete(lookupKey: String): Int

    @Query("SELECT COUNT(*) FROM merchant_memory")
    suspend fun getMemoryCount(): Int

    @Transaction
    suspend fun rememberMerchant(
        lookupKey: String,
        category: String,
        customAlias: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val existing = getMerchantMemory(lookupKey)
        if (existing != null) {
            val updated = existing.copy(
                category = category,
                customAlias = customAlias ?: existing.customAlias,
                transactionCount = existing.transactionCount + 1,
                lastUpdated = timestamp
            )
            update(updated)
        } else {
            val newMemory = MerchantMemoryEntity(
                lookupKey = lookupKey,
                category = category,
                customAlias = customAlias,
                transactionCount = 1,
                lastUpdated = timestamp
            )
            upsert(newMemory)
        }
    }
}
