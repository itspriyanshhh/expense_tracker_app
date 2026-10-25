package com.example.pulseledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pre-debit upcoming mandate entity representing scheduled auto-debits,
 * e-mandates, and recurring subscriptions (e.g. OpenAI, Netflix, SIPs).
 */
@Entity(
    tableName = "upcoming_mandates",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["isPaid"]),
        Index(value = ["isDismissed"])
    ]
)
data class UpcomingMandateEntity(
    @PrimaryKey
    val id: String, // e.g. "mandate_openai_1999_timestamp"
    val merchant: String,
    val amount: Double,
    val dueDate: Long, // Epoch timestamp in ms when debit is scheduled
    val rawText: String,
    val isPaid: Boolean = false,
    val isDismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
