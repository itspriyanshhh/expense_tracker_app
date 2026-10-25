package com.example.pulseledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    DEBIT,
    CREDIT,
    TRANSFER_NEUTRAL
}

enum class VerificationState {
    VERIFIED_MEMORY,
    VERIFIED_RULE,
    VERIFIED_AI,
    VERIFIED_USER,
    PENDING_USER_TAP
}

/**
 * Immutable transaction record captured via notification listener or banking SMS.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["verificationState"]),
        Index(value = ["category"]),
        Index(value = ["upiVpa"]),
        Index(value = ["rawMerchant"]),
        Index(value = ["upiRefNo"]),
        Index(value = ["parentTxId"]),
        Index(value = ["eventTag"]),
        Index(value = ["isRefund"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String, // Deterministic hash or 12-digit UPI RRN / Ref
    val amount: Double,
    val netAmount: Double = amount,
    val type: TransactionType,
    val rawMerchant: String,
    val displayMerchant: String,
    val upiVpa: String? = null,
    val upiRefNo: String? = null,
    val bankOrAppSource: String, // e.g. "GPay", "PhonePe", "Paytm", "HDFC Bank", "SBI"
    val category: String,
    val timestamp: Long,
    val verificationState: VerificationState,
    val parentTxId: String? = null,
    val isRefund: Boolean = false,
    val eventTag: String? = null
)

