package com.example.pulseledger.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.service.ActionableNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for 1-Tap Split Bill Notification Actions:
 * - [🔗 Offset Split Bill]: Links parentTxId and subtracts credit.amount from parent debit's netAmount.
 * - [💰 Keep as Income]: Keeps the credit transaction as verified Income.
 */
class SplitBillActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val creditTxId = intent.getStringExtra(EXTRA_CREDIT_TX_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        // Cancel notification immediately
        if (notificationId != 0) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        val appContext = context.applicationContext
        val db = PulseDatabase.getInstance(appContext)
        val transactionDao = db.transactionDao()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_OFFSET_SPLIT_BILL -> {
                        val parentDebitId = intent.getStringExtra(EXTRA_PARENT_DEBIT_ID) ?: return@launch
                        val offsetAmount = intent.getDoubleExtra(EXTRA_OFFSET_AMOUNT, 0.0)
                        val merchantName = intent.getStringExtra(EXTRA_MERCHANT_NAME) ?: "original expense"

                        transactionDao.linkSplitBill(
                            creditTxId = creditTxId,
                            parentDebitId = parentDebitId,
                            offsetAmount = offsetAmount
                        )

                        ActionableNotificationHelper.postConfirmationNotification(
                            appContext,
                            merchantName,
                            "Offset ₹${offsetAmount.toInt()} against $merchantName"
                        )
                    }
                    ACTION_KEEP_AS_INCOME -> {
                        val creditTx = transactionDao.getTransactionById(creditTxId)
                        if (creditTx != null) {
                            transactionDao.updateTransaction(
                                creditTx.copy(
                                    category = "income",
                                    verificationState = VerificationState.VERIFIED_USER
                                )
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_OFFSET_SPLIT_BILL = "com.pulseledger.app.ACTION_OFFSET_SPLIT_BILL"
        const val ACTION_KEEP_AS_INCOME = "com.pulseledger.app.ACTION_KEEP_AS_INCOME"

        const val EXTRA_CREDIT_TX_ID = "extra_credit_tx_id"
        const val EXTRA_PARENT_DEBIT_ID = "extra_parent_debit_id"
        const val EXTRA_OFFSET_AMOUNT = "extra_offset_amount"
        const val EXTRA_MERCHANT_NAME = "extra_merchant_name"
        const val EXTRA_SENDER_NAME = "extra_sender_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
