package com.dhiraj.expensetracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dhiraj.expensetracker.notifications.TransactionNotificationManager
import com.dhiraj.expensetracker.receivers.CategoryActionReceiver
import com.dhiraj.expensetracker.ui.components.CategoryOverlaySheet
import com.dhiraj.expensetracker.ui.theme.AppPreferences
import com.dhiraj.expensetracker.ui.theme.ExpenseTrackerTheme
import com.dhiraj.expensetracker.ui.theme.ThemePreferences

class CategorizeTransactionActivity : ComponentActivity() {

    private var amount = 0.0
    private var merchant = ""
    private var bank = ""
    private var upiRef: String? = null
    private var rawSms: String? = null
    private var date: Long = System.currentTimeMillis()
    private var notificationId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        amount = intent.getDoubleExtra("amount", 0.0)
        merchant = intent.getStringExtra("merchant") ?: ""
        bank = intent.getStringExtra("bank") ?: ""
        upiRef = intent.getStringExtra("upi_ref")
        rawSms = intent.getStringExtra("raw_sms")
        date = intent.getLongExtra("date", System.currentTimeMillis())
        notificationId = intent.getIntExtra("notification_id", -1)

        if (notificationId != -1) {
            TransactionNotificationManager.cancelNotification(this, notificationId)
        }

        setContent {
            ExpenseTrackerTheme(
                darkTheme = ThemePreferences.isDarkModeEnabled(this),
                palette = AppPreferences.getPalette(this),
                dynamicColor = false
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CategoryOverlaySheet(
                        merchantHint = merchant,
                        amountHint = amount,
                        onDismiss = {
                            finish()
                        },
                        onCategorySelected = { category ->
                            saveTransaction(category)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun saveTransaction(category: String) {
        val intent = Intent(this, CategoryActionReceiver::class.java).apply {
            action = "CATEGORIZE_TRANSACTION"
            putExtra("notification_id", notificationId)
            putExtra("amount", amount)
            putExtra("merchant", merchant)
            putExtra("bank", bank)
            putExtra("category", category)
            putExtra("upi_ref", upiRef)
            putExtra("raw_sms", rawSms)
            putExtra("date", date)
        }
        sendBroadcast(intent)
    }
}
