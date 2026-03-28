package com.dhiraj.expensetracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.notifications.TransactionNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class CategoryActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "CATEGORIZE_TRANSACTION") return

        val notificationId = intent.getIntExtra("notification_id", -1)

        val amount = intent.getDoubleExtra("amount", 0.0)
        val merchant = intent.getStringExtra("merchant") ?: "Unknown"
        val bank = intent.getStringExtra("bank")
        val category = intent.getStringExtra("category") ?: "Other"
        val upiRef = intent.getStringExtra("upi_ref")
        val rawSms = intent.getStringExtra("raw_sms")
        val timestamp = intent.getLongExtra("date", System.currentTimeMillis())

        val transaction = Transaction(
            amount = amount,
            merchantName = merchant,
            category = category,
            date = Date(timestamp),
            bankName = bank,
            upiId = upiRef,
            rawSmsText = rawSms,
            isManualEntry = false
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)

                // Insert transaction.
                db.transactionDao().insertTransaction(transaction)

                // Update/cancel only when this came from a real notification action.
                if (notificationId != -1) {
                    db.notificationLogDao().updateStatus(notificationId, "LOGGED")
                    TransactionNotificationManager.cancelNotification(context, notificationId)
                }

                TransactionNotificationManager.showSuccessNotification(
                    context = context,
                    amount = amount,
                    category = category
                )

                Log.d(TAG, "Transaction logged successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save transaction", e)
            }
        }
    }

    companion object {
        private const val TAG = "CategoryActionReceiver"
    }
}
