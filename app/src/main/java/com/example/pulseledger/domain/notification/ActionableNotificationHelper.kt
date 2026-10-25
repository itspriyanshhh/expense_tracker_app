package com.example.pulseledger.domain.notification

import android.content.Context
import com.example.pulseledger.data.local.entity.TransactionEntity

/**
 * Facade delegating to the interactive notification helper in com.example.pulseledger.service.
 */
object ActionableNotificationHelper {
    const val CHANNEL_ID = com.example.pulseledger.service.ActionableNotificationHelper.QUICK_TAG_CHANNEL

    fun createNotificationChannel(context: Context) {
        com.example.pulseledger.service.ActionableNotificationHelper.createNotificationChannels(context)
    }

    fun fireQuickTagNotification(context: Context, transaction: TransactionEntity) {
        com.example.pulseledger.service.ActionableNotificationHelper.fireQuickTagNotification(context, transaction)
    }

    fun fireSplitBillNotification(context: Context, creditTx: TransactionEntity, parentDebit: TransactionEntity) {
        com.example.pulseledger.service.ActionableNotificationHelper.fireSplitBillNotification(context, creditTx, parentDebit)
    }
}
