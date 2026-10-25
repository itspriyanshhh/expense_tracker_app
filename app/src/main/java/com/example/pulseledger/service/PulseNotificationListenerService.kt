package com.example.pulseledger.service

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.repository.ExpenseWaterfallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background Notification Ingestion Engine.
 *
 * Intercepts notifications from major Indian UPI payment apps (GPay, PhonePe, Paytm, BHIM, Amazon Pay)
 * and SMS messaging apps in real-time, extracting transaction metadata and feeding them into
 * the 4-Tier Categorization Waterfall with 3-minute sliding window deduplication.
 *
 * Implements [onListenerDisconnected] with [requestRebind] to resist OEM background process killing
 * (MIUI, OneUI, ColorOS, OxygenOS).
 */
class PulseNotificationListenerService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(job + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val defaultSmsPackage = try {
            Telephony.Sms.getDefaultSmsPackage(this)
        } catch (e: Exception) {
            null
        }

        // Allowlist verification
        val isAllowedPackage = packageName in ALLOWED_PACKAGES || packageName == defaultSmsPackage
        if (!isAllowedPackage) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        // Extract most descriptive body
        val messageBody = buildString {
            if (title.isNotBlank()) {
                append(title)
                append("\n")
            }
            if (bigText.isNotBlank()) {
                append(bigText)
            } else if (text.isNotBlank()) {
                append(text)
            }
        }.trim()

        if (messageBody.isBlank()) return

        serviceScope.launch {
            try {
                val db = PulseDatabase.getInstance(applicationContext)
                val repository = ExpenseWaterfallRepository(
                    context = applicationContext,
                    transactionDao = db.transactionDao(),
                    merchantMemoryDao = db.merchantMemoryDao()
                )

                repository.processRawText(
                    text = messageBody,
                    senderOrPackage = packageName,
                    timestamp = sbn.postTime
                )
            } catch (e: Exception) {
                // Defensive catch to prevent notification listener crashes
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, PulseNotificationListenerService::class.java))
        }
    }

    companion object {
        /**
         * Supported Indian Fintech and Messaging package identifiers.
         */
        val ALLOWED_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay (Tez)
            "com.phonepe.app",                       // PhonePe
            "net.one97.paytm",                       // Paytm
            "in.org.npci.upiapp",                    // BHIM UPI (NPCI)
            "in.amazon.mShop.android.shopping",      // Amazon Pay
            "com.google.android.apps.messaging",     // Google Messages (Default SMS)
            "com.samsung.android.messaging"          // Samsung Messages
        )
    }
}
