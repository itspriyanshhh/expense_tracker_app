package com.example.pulseledger.domain.notification

import android.content.Context
import com.example.pulseledger.data.local.entity.TransactionEntity

interface NotificationTrigger {
    fun fireQuickTagNotification(transaction: TransactionEntity)
    fun fireSplitBillNotification(creditTx: TransactionEntity, parentDebit: TransactionEntity)
}

class AndroidNotificationTrigger(private val context: Context) : NotificationTrigger {
    override fun fireQuickTagNotification(transaction: TransactionEntity) {
        ActionableNotificationHelper.fireQuickTagNotification(context, transaction)
    }

    override fun fireSplitBillNotification(creditTx: TransactionEntity, parentDebit: TransactionEntity) {
        ActionableNotificationHelper.fireSplitBillNotification(context, creditTx, parentDebit)
    }
}
