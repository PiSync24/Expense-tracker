package com.dhiraj.expensetracker.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.NotificationLog
import com.dhiraj.expensetracker.utils.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BankNotificationListener : NotificationListenerService() {

    private var lastSignature: String? = null
    private var lastTimestamp: Long = 0L

    companion object {
        private const val DUPLICATE_WINDOW_MS = 15_000L
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        val parsed = SmsParser.parse(sbn.packageName, "$title $text") ?: return

        val db = AppDatabase.getDatabase(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {

            // ðŸ” DB-level dedupe (FINAL authority)
            val upiRef = parsed.upiReference ?: return@launch
            val exists = db.notificationLogDao().existsPendingByUpi(upiRef)



            if (exists) return@launch

            val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            // 1ï¸âƒ£ Show notification
            TransactionNotificationManager.showTransactionNotification(
                applicationContext,
                parsed,
                notificationId
            )

            // 2ï¸âƒ£ Save log
            db.notificationLogDao().insert(
                NotificationLog(
                    notificationId = notificationId,
                    merchant = parsed.merchantName ?: "Unknown",
                    amount = parsed.amount,
                    timestamp = System.currentTimeMillis(),
                    status = "PENDING",
                    source = "NOTIFICATION",
                    upiReference = parsed.upiReference
                )
            )
        }
    }


    // ---------- helpers ----------

    private fun isMerchantConfirmation(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        if (combined.contains("debited") || combined.contains("credited")) return false

        val keywords = listOf(
            "order confirmed", "payment confirmed", "delivery",
            "out for delivery", "track", "tracking", "invoice"
        )
        return keywords.any { combined.contains(it) }
    }

    private fun isMerchantAppSource(packageName: String): Boolean {
        val merchants = listOf(
            "swiggy", "zomato", "zepto", "blinkit",
            "instamart", "myntra", "uber", "ola"
        )
        return merchants.any { packageName.contains(it, true) }
    }

    private fun isTrustedFinancialSource(packageName: String): Boolean {
        val keywords = listOf(
            "bank", "upi", "paytm", "phonepe", "gpay",
            "google.android.apps.messaging",
            "axis", "hdfc", "icici", "sbi", "kotak"
        )
        return keywords.any { packageName.contains(it, true) }
    }
}

