package com.dhiraj.expensetracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.NotificationLog
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.notifications.TransactionNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun CategoryPickerDialog(
    log: NotificationLog,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val categoryDao = remember(db) { db.categoryDao() }
    val scope = rememberCoroutineScope()

    val categories by categoryDao
        .getAllCategories()
        .collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            if (categories.isEmpty()) {
                Text("No categories found")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(categories, key = { it.id }) { category ->
                        TextButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    saveTransactionFromLog(
                                        db = db,
                                        context = context,
                                        log = log,
                                        category = category.name
                                    )
                                }
                                onDismiss()
                            }
                        ) {
                            Text("${category.emoji} ${category.name}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun saveTransactionFromLog(
    db: AppDatabase,
    context: Context,
    log: NotificationLog,
    category: String
) {
    db.transactionDao().insertTransaction(
        Transaction(
            amount = log.amount,
            merchantName = log.merchant,
            category = category,
            date = Date(log.timestamp),
            bankName = null,
            upiId = log.upiReference,
            rawSmsText = null,
            isManualEntry = false
        )
    )

    db.notificationLogDao().updateStatus(log.notificationId, "LOGGED")

    TransactionNotificationManager.cancelNotification(
        context,
        log.notificationId
    )
}
