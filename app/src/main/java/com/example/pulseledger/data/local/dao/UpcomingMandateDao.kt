package com.example.pulseledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pulseledger.data.local.entity.UpcomingMandateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpcomingMandateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandate(mandate: UpcomingMandateEntity)

    @Update
    suspend fun updateMandate(mandate: UpcomingMandateEntity)

    /**
     * Active upcoming mandates that are neither marked as paid nor dismissed.
     */
    @Query("""
        SELECT * FROM upcoming_mandates 
        WHERE isPaid = 0 AND isDismissed = 0 
        ORDER BY dueDate ASC
    """)
    fun getActiveMandatesFlow(): Flow<List<UpcomingMandateEntity>>

    @Query("SELECT * FROM upcoming_mandates WHERE id = :id LIMIT 1")
    suspend fun getMandateById(id: String): UpcomingMandateEntity?

    @Query("UPDATE upcoming_mandates SET isPaid = 1 WHERE id = :id")
    suspend fun markAsPaid(id: String): Int

    @Query("UPDATE upcoming_mandates SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissMandate(id: String): Int

    @Query("DELETE FROM upcoming_mandates WHERE id = :id")
    suspend fun deleteMandate(id: String): Int
}
