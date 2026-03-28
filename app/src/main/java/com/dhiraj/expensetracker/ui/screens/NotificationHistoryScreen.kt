package com.dhiraj.expensetracker.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.NotificationLog
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.notifications.TransactionNotificationManager
import com.dhiraj.expensetracker.ui.components.CategoryOverlaySheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class NotificationDayGroup(
    val date: LocalDate,
    val logs: List<NotificationLog>,
    val debitTotal: Double,
    val creditTotal: Double,
    val pendingCount: Int
) {
    val totalVolume: Double
        get() = debitTotal + creditTotal
}

private val DebitColor = Color(0xFFB85C5C)
private val CreditColor = Color(0xFF2E8B57)
private val logTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
private val logDayFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale.getDefault())

@Composable
fun NotificationHistoryScreen() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val dao = remember(db) { db.notificationLogDao() }
    val scope = rememberCoroutineScope()

    val logs: List<NotificationLog> by dao
        .getAllLogs()
        .collectAsState(initial = emptyList())

    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No notifications yet")
        }
        return
    }

    val grouped = remember(logs) {
        logs
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { (date, dayLogs) ->
                NotificationDayGroup(
                    date = date,
                    logs = dayLogs.sortedByDescending { it.timestamp },
                    debitTotal = dayLogs.filter { it.amount < 0 }.sumOf { abs(it.amount) },
                    creditTotal = dayLogs.filter { it.amount >= 0 }.sumOf { abs(it.amount) },
                    pendingCount = dayLogs.count { it.status == "PENDING" }
                )
            }
            .sortedByDescending { it.date }
    }
    val recentGroups = remember(grouped) { grouped.take(7) }
    val maxRecentVolume = remember(recentGroups) {
        recentGroups.maxOfOrNull { it.totalVolume } ?: 0.0
    }

    var selectedLog by remember { mutableStateOf<NotificationLog?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "distribution") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Day-wise Distribution (Last 7 Days)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    recentGroups.forEach { dayGroup ->
                        val progress = if (maxRecentVolume > 0) {
                            (dayGroup.totalVolume / maxRecentVolume).toFloat()
                        } else {
                            0f
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dayGroup.date.format(logDayFormatter),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${String.format("%.0f", dayGroup.totalVolume)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Debit ₹${String.format("%.0f", dayGroup.debitTotal)} | Credit ₹${String.format("%.0f", dayGroup.creditTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pending ${dayGroup.pendingCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        grouped.forEach { dayGroup ->
            item(key = "day-${dayGroup.date}") {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Daily Logs - ${dayGroup.date.format(logDayFormatter)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Logs: ${dayGroup.logs.size} | Pending: ${dayGroup.pendingCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "D ₹${String.format("%.0f", dayGroup.debitTotal)} / C ₹${String.format("%.0f", dayGroup.creditTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(items = dayGroup.logs, key = { it.id }) { log ->
                val isDebit = log.amount < 0
                val amountColor = if (isDebit) DebitColor else CreditColor
                val prefix = if (isDebit) "⬇" else "⬆"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (log.status == "PENDING") {
                                selectedLog = log
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.merchant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "$prefix ₹${String.format("%.2f", abs(log.amount))}",
                                color = amountColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatLogTime(log.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = log.status,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (log.status == "PENDING") Color(0xFFF39C12) else Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedLog?.let { log ->
        CategoryOverlaySheet(
            merchantHint = log.merchant,
            amountHint = log.amount,
            onDismiss = { selectedLog = null },
            onCategorySelected = { category ->
                scope.launch(Dispatchers.IO) {
                    saveTransactionFromLog(
                        db = db,
                        context = context,
                        log = log,
                        category = category
                    )
                }
                selectedLog = null
            }
        )
    }
}

private fun formatLogTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(logTimeFormatter)
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
    TransactionNotificationManager.cancelNotification(context, log.notificationId)
}
