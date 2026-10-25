package com.example.pulseledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.dao.UpcomingMandateDao
import com.example.pulseledger.data.local.entity.MerchantMemoryEntity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.UpcomingMandateEntity

@Database(
    entities = [
        TransactionEntity::class,
        MerchantMemoryEntity::class,
        UpcomingMandateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMemoryDao(): MerchantMemoryDao
    abstract fun upcomingMandateDao(): UpcomingMandateDao

    companion object {
        private const val DATABASE_NAME = "pulseledger.db"

        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
