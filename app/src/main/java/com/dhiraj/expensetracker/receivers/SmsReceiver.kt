package com.dhiraj.expensetracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.dhiraj.expensetracker.utils.SmsParser
import com.dhiraj.expensetracker.notifications.TransactionNotificationManager
import com.dhiraj.expensetracker.utils.ParsedTransaction
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.NotificationLog
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (msg in messages) {

            val parsed = SmsParser.parse(
                msg.displayOriginatingAddress ?: return,
                msg.messageBody ?: return
            ) ?: continue

            if (parsed.confidence < 0.5f) continue
            if (isDuplicateTransaction(context, parsed)) continue

            val notificationId =
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            // 🔔 Show notification
            TransactionNotificationManager.showTransactionNotification(
                context,
                parsed,
                notificationId
            )

            // 📝 Save log
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(context)
                    .notificationLogDao()
                    .insert(
                        NotificationLog(
                            notificationId = notificationId,
                            merchant = parsed.merchantName ?: "Unknown",
                            amount = parsed.amount,
                            timestamp = System.currentTimeMillis(),
                            status = "PENDING",
                            source = "SMS",
                            upiReference = parsed.upiReference
                        )
                    )
            }
        }
    }

}

/**
 * Prevents duplicate transactions from being processed
 */
fun isDuplicateTransaction(
    context: Context,
    tx: ParsedTransaction
): Boolean {

    val upiRef = tx.upiReference ?: return false
    val db = AppDatabase.getDatabase(context)

    return runBlocking {
        // 1️⃣ Already saved as transaction
        if (db.transactionDao().existsByUpiRef(upiRef)) {
            true
        }
        // 2️⃣ Already pending in notification log
        else {
            db.notificationLogDao().existsPendingByUpi(upiRef)
        }
    }
}




