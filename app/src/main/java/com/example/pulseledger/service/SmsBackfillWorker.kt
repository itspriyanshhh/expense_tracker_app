package com.example.pulseledger.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.pulseledger.data.local.PulseDatabase
import com.example.pulseledger.data.remote.GeminiClassifierClient
import com.example.pulseledger.data.repository.ExpenseWaterfallRepository
import com.example.pulseledger.data.security.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Historical SMS Ingestion Worker.
 *
 * Scans Telephony SMS Inbox for the past 30 days (if optional READ_SMS permission is granted)
 * and processes transactions through the 4-tier waterfall with sliding-window deduplication.
 *
 * Privacy & Politeness Guardrail:
 * Explicitly passes notificationTrigger = null to ExpenseWaterfallRepository so that historical backfills
 * run silently without firing notification spam.
 */
class SmsBackfillWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Safe permission check
        val hasPermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            // Gracefully succeed if user chose not to grant optional SMS permission
            return@withContext Result.failure()
        }

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC" // Process chronologically

        val db = PulseDatabase.getInstance(applicationContext)
        val keyStorage = SecureKeyStorage(applicationContext)

        // notificationTrigger = null ensures ZERO push notifications are fired during backfill
        val repository = ExpenseWaterfallRepository(
            transactionDao = db.transactionDao(),
            merchantMemoryDao = db.merchantMemoryDao(),
            upcomingMandateDao = db.upcomingMandateDao(),
            keyStorage = keyStorage,
            geminiClassifier = GeminiClassifierClient(keyStorage),
            notificationTrigger = null
        )

        var scannedCount = 0
        var importedCount = 0

        try {
            val cursor = applicationContext.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    val body = if (bodyIndex != -1) c.getString(bodyIndex) else null
                    val address = if (addressIndex != -1) c.getString(addressIndex) else null
                    val date = if (dateIndex != -1) c.getLong(dateIndex) else System.currentTimeMillis()

                    if (!body.isNullOrBlank()) {
                        scannedCount++
                        val result = repository.processRawText(
                            text = body,
                            senderOrPackage = address,
                            timestamp = date
                        )
                        if (result != null) {
                            importedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure()
        }

        // Persist scan statistics to SharedPreferences
        try {
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_BACKFILL_TIME, System.currentTimeMillis())
                .putInt(KEY_LAST_SCANNED_COUNT, scannedCount)
                .putInt(KEY_LAST_IMPORTED_COUNT, importedCount)
                .apply()
        } catch (_: Exception) {}

        val output = workDataOf(
            KEY_SCANNED to scannedCount,
            KEY_IMPORTED to importedCount
        )
        Result.success(output)
    }

    companion object {
        const val WORK_NAME = "pulse_historical_sms_backfill"
        const val PREFS_NAME = "pulse_sms_backfill_prefs"
        const val KEY_LAST_BACKFILL_TIME = "last_backfill_time"
        const val KEY_LAST_SCANNED_COUNT = "last_scanned_count"
        const val KEY_LAST_IMPORTED_COUNT = "last_imported_count"
        const val KEY_SCANNED = "key_scanned_count"
        const val KEY_IMPORTED = "key_imported_count"

        /**
         * Enqueues a historical SMS backfill task.
         * @param force If true, replaces any existing/completed task so the backfill runs afresh.
         */
        fun enqueue(context: Context, force: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
