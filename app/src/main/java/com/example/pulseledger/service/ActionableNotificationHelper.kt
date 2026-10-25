package com.example.pulseledger.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pulseledger.MainActivity
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.ui.model.PulseCategory
import java.util.Calendar
import kotlin.math.abs

/**
 * Interactive Notification System for Tier 4 Unverified / Ambiguous P2P Transactions.
 *
 * Provides context-aware 1-Tap quick-categorization actions based on transaction amount
 * and hour of day, allowing users to categorize and permanently memorize vendor rules directly
 * from their lock screen or notification shade.
 */
object ActionableNotificationHelper {

    const val QUICK_TAG_CHANNEL = "pulse_quick_tag_channel"
    private const val QUICK_TAG_CHANNEL_NAME = "1-Tap Expense Categorization"

    const val CONFIRMATION_CHANNEL = "pulse_confirmation_channel"
    private const val CONFIRMATION_CHANNEL_NAME = "Categorization Confirmations"

    data class ContextualAction(
        val label: String,
        val category: PulseCategory,
        val aliasLabel: String
    )

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // High priority channel for 1-Tap actionable categorization
            val quickTagChannel = NotificationChannel(
                QUICK_TAG_CHANNEL,
                QUICK_TAG_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "1-Tap actionable notifications to categorize ambiguous payments and memorize vendor rules"
                enableVibration(true)
            }
            manager.createNotificationChannel(quickTagChannel)

            // Low priority channel for subtle confirmation notifications
            val confirmChannel = NotificationChannel(
                CONFIRMATION_CHANNEL,
                CONFIRMATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent confirmations when a vendor rule is permanently remembered"
                enableVibration(false)
            }
            manager.createNotificationChannel(confirmChannel)
        }
    }

    /**
     * Resolves 3 context-aware category suggestions based on amount and time of day.
     */
    fun getContextualActions(amount: Double, timestamp: Long = System.currentTimeMillis()): List<ContextualAction> {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return if (amount < 100.0) {
            // Micro & Daily Expenses (< ₹100): Chai, Auto/Metro, Snacks, Grocery
            listOf(
                ContextualAction("☕ Food/Tea", PulseCategory.FOOD_AND_DINING, "Food/Tea"),
                ContextualAction("🛺 Transit", PulseCategory.TRANSIT_AND_TRAVEL, "Transit"),
                ContextualAction("🛒 Grocery", PulseCategory.GROCERIES, "Grocery")
            )
        } else if (amount >= 5000.0) {
            // High-value transactions (₹5,000+): Investments, Rent, Shopping
            listOf(
                ContextualAction("📈 Invest", PulseCategory.INVESTMENTS, "Investments"),
                ContextualAction("🏠 Rent", PulseCategory.HOME_AND_RENT, "Home & Rent"),
                ContextualAction("🛍️ Shopping", PulseCategory.SHOPPING, "Shopping")
            )
        } else {
            // Regular transactions (₹100 to ₹5,000): Dining, Shopping, Bills / Personal Care
            when (hour) {
                in 20..23, in 0..4 -> listOf(
                    ContextualAction("🍽️ Dining", PulseCategory.FOOD_AND_DINING, "Dining"),
                    ContextualAction("🛍️ Shopping", PulseCategory.SHOPPING, "Shopping"),
                    ContextualAction("🎉 Entertainment", PulseCategory.ENTERTAINMENT, "Entertainment")
                )
                else -> listOf(
                    ContextualAction("🍽️ Dining", PulseCategory.FOOD_AND_DINING, "Dining"),
                    ContextualAction("🛍️ Shopping", PulseCategory.SHOPPING, "Shopping"),
                    ContextualAction("🧾 Bills", PulseCategory.BILLS_AND_UTILITIES, "Bills")
                )
            }
        }
    }

    /**
     * Posts a high-priority 1-Tap actionable notification with 3 context-aware category actions.
     */
    fun fireQuickTagNotification(context: Context, transaction: TransactionEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val notificationId = abs(transaction.id.hashCode())
        val lookupKey = transaction.upiVpa?.trim()?.lowercase() ?: transaction.rawMerchant.trim().lowercase()
        val actions = getContextualActions(transaction.amount, transaction.timestamp)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Paid ₹${transaction.amount.toInt()} to ${transaction.displayMerchant}"
        val subtitle = "Tap once to categorize & remember ${transaction.displayMerchant} forever:"

        val builder = NotificationCompat.Builder(context, QUICK_TAG_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        actions.forEachIndexed { index, action ->
            val actionIntent = Intent(context, CategoryActionReceiver::class.java).apply {
                putExtra(CategoryActionReceiver.EXTRA_TX_ID, transaction.id)
                putExtra(CategoryActionReceiver.EXTRA_LOOKUP_KEY, lookupKey)
                putExtra(CategoryActionReceiver.EXTRA_CATEGORY, action.category.id)
                putExtra(CategoryActionReceiver.EXTRA_CATEGORY_LABEL, action.aliasLabel)
                putExtra(CategoryActionReceiver.EXTRA_MERCHANT_NAME, transaction.displayMerchant)
                putExtra(CategoryActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }

            val pendingAction = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + index,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, action.label, pendingAction)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Step 8.5 P2P Split-Bill Linker Actionable Notification:
     * "Received ₹{amount} from {sender} — Offset against ₹{debitAmount} at {debitMerchant}?"
     * Action 1: [🔗 Offset Split Bill]
     * Action 2: [💰 Keep as Income]
     */
    fun fireSplitBillNotification(
        context: Context,
        creditTx: TransactionEntity,
        parentDebit: TransactionEntity
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val notificationId = abs(("split_${creditTx.id}_${parentDebit.id}").hashCode())
        val creditAmountInt = creditTx.amount.toInt()
        val debitAmountInt = parentDebit.amount.toInt()
        val sender = creditTx.displayMerchant
        val debitMerchant = parentDebit.displayMerchant

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Received ₹$creditAmountInt from $sender"
        val subtitle = "Offset against ₹$debitAmountInt at $debitMerchant?"

        // Action 1: Offset Split Bill
        val offsetIntent = Intent(context, com.example.pulseledger.domain.notification.SplitBillActionReceiver::class.java).apply {
            action = com.example.pulseledger.domain.notification.SplitBillActionReceiver.ACTION_OFFSET_SPLIT_BILL
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_CREDIT_TX_ID, creditTx.id)
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_PARENT_DEBIT_ID, parentDebit.id)
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_OFFSET_AMOUNT, creditTx.amount)
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_MERCHANT_NAME, debitMerchant)
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_SENDER_NAME, sender)
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val offsetPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            offsetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Keep as Income
        val incomeIntent = Intent(context, com.example.pulseledger.domain.notification.SplitBillActionReceiver::class.java).apply {
            action = com.example.pulseledger.domain.notification.SplitBillActionReceiver.ACTION_KEEP_AS_INCOME
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_CREDIT_TX_ID, creditTx.id)
            putExtra(com.example.pulseledger.domain.notification.SplitBillActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val incomePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            incomeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, QUICK_TAG_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "🔗 Offset Split Bill", offsetPendingIntent)
            .addAction(0, "💰 Keep as Income", incomePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Ignored
        }
    }

    /**
     * Posts a subtle, silent confirmation notification after user taps an inline category action.
     */
    fun postConfirmationNotification(context: Context, merchantName: String, categoryLabel: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val confirmId = abs("confirm_$merchantName".hashCode())
        val message = "Got it! $categoryLabel."

        val builder = NotificationCompat.Builder(context, CONFIRMATION_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("SpendLog Updated")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(6000) // Auto-dismiss after 6 seconds

        try {
            NotificationManagerCompat.from(context).notify(confirmId, builder.build())
        } catch (e: SecurityException) {
            // Ignored
        }
    }
}
