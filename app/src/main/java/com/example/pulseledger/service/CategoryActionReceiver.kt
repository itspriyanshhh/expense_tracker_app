package com.example.pulseledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.pulseledger.data.local.PulseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BroadcastReceiver triggered when a user taps an inline category action button
 * on a Tier 4 actionable push notification.
 *
 * Atomically commits the categorization rule to Tier 1 merchant_memory, updates the transaction,
 * dismisses the active notification, and provides positive confirmation feedback.
 */
class CategoryActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        val lookupKey = intent.getStringExtra(EXTRA_LOOKUP_KEY) ?: return
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return
        val categoryLabel = intent.getStringExtra(EXTRA_CATEGORY_LABEL) ?: category
        val merchantName = intent.getStringExtra(EXTRA_MERCHANT_NAME) ?: lookupKey
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PulseDatabase.getInstance(context)
                db.transactionDao().resolveAndRememberMerchant(
                    txId = txId,
                    lookupKey = lookupKey,
                    category = category,
                    customAlias = merchantName
                )

                // Dismiss the prompt notification
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                // Show feedback toast on Main thread
                withContext(Dispatchers.Main) {
                    val toastMessage = "Got it! Future payments to $merchantName will auto-tag as $categoryLabel."
                    Toast.makeText(context.applicationContext, toastMessage, Toast.LENGTH_LONG).show()
                }

                // Also post subtle confirmation notification
                ActionableNotificationHelper.postConfirmationNotification(
                    context = context,
                    merchantName = merchantName,
                    categoryLabel = categoryLabel
                )
            } catch (e: Exception) {
                // Log or handle gracefully
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TX_ID = "extra_tx_id"
        const val EXTRA_LOOKUP_KEY = "extra_lookup_key"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_CATEGORY_LABEL = "extra_category_label"
        const val EXTRA_MERCHANT_NAME = "extra_merchant_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
