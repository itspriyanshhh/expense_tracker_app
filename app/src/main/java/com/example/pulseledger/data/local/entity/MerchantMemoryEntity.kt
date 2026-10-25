package com.example.pulseledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tier 1 Memory Cache: Exact-match persistent dictionary for repeat vendors and memorized P2P contacts.
 *
 * When a user categorizes a transaction or customizes a contact name,
 * this table ensures future transactions are recognized with 100% accuracy and zero network cost.
 */
@Entity(
    tableName = "merchant_memory",
    indices = [
        Index(value = ["category"]),
        Index(value = ["lastUpdated"])
    ]
)
data class MerchantMemoryEntity(
    @PrimaryKey
    val lookupKey: String, // Normalized lowercase UPI VPA (e.g. "9876543210@ybl") or normalized merchant name
    val category: String,
    val customAlias: String? = null, // Custom user label (e.g. "Local Chai / Ramesh")
    val transactionCount: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)
