package com.example.pulseledger.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.pulseledger.data.local.PulseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when a user taps one of the 1-Tap category action buttons
 * on a Tier 4 Android Push Notification.
 */
class QuickTagActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        val lookupKey = intent.getStringExtra(EXTRA_LOOKUP_KEY) ?: return
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PulseDatabase.getInstance(context)
                db.transactionDao().resolveAndRememberMerchant(
                    txId = txId,
                    lookupKey = lookupKey,
                    category = category
                )

                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TX_ID = "extra_tx_id"
        const val EXTRA_LOOKUP_KEY = "extra_lookup_key"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
