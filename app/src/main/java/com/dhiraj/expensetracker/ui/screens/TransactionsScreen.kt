package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.FinancialPlanEntry
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.ui.utils.TransactionClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private data class DayGroup(
    val date: LocalDate,
    val transactions: List<Transaction>
)

@Composable
fun TransactionsScreen() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val transactionDao = remember(db) { db.transactionDao() }
    val planDao = remember(db) { db.financialPlanDao() }

    val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())
    val upcomingPlans by planDao.getEntriesForTemplate(1).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var monthOffset by remember { mutableIntStateOf(0) }
    var editingTxn by remember { mutableStateOf<Transaction?>(null) }

    val viewedMonth = YearMonth.now().plusMonths(monthOffset.toLong())

    val monthTransactions = remember(transactions, monthOffset) {
        transactions.filter {
            val localDate = it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.year == viewedMonth.year && localDate.monthValue == viewedMonth.monthValue
        }
    }

    val grouped = remember(monthTransactions) {
        monthTransactions
            .groupBy { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { DayGroup(date = it.key, transactions = it.value.sortedByDescending { t -> t.date }) }
            .sortedByDescending { it.date }
    }

    val expense = remember(monthTransactions) {
        monthTransactions.filterNot { TransactionClassifier.isCredit(it) }.sumOf { abs(it.amount) }
    }
    val income = remember(monthTransactions) {
        monthTransactions.filter { TransactionClassifier.isCredit(it) }.sumOf { abs(it.amount) }
    }
    val total = income - expense

    val safeSpend = remember(expense, income, viewedMonth) {
        val today = LocalDate.now()
        val monthEnd = viewedMonth.atEndOfMonth()
        val days = if (viewedMonth == YearMonth.from(today)) {
            (monthEnd.dayOfMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        } else {
            viewedMonth.lengthOfMonth()
        }
        ((income - expense).coerceAtLeast(0.0)) / days
    }

    val topCategory = remember(monthTransactions) {
        monthTransactions
            .filterNot { TransactionClassifier.isCredit(it) }
            .groupBy { it.category.ifBlank { "Other" } }
            .maxByOrNull { (_, list) -> list.sumOf { abs(it.amount) } }
            ?.key
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MonthHeader(
                monthText = viewedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                onPrev = { monthOffset-- },
                onNext = { if (monthOffset < 1) monthOffset++ }
            )
        }

        item {
            SummaryCard(expense = expense, income = income, total = total)
        }

        item {
            SmartCards(
                safeSpend = safeSpend,
                topCategory = topCategory,
                upcomingPlans = upcomingPlans
            )
        }

        if (grouped.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("No records for this month", modifier = Modifier.padding(14.dp))
                }
            }
        }

        grouped.forEach { group ->
            item(key = "header-${group.date}") {
                Text(
                    group.date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(group.transactions, key = { it.id }) { txn ->
                LedgerRow(
                    transaction = txn,
                    onConfirmAuto = { toConfirm ->
                        scope.launch(Dispatchers.IO) {
                            val nextNotes = (toConfirm.notes.orEmpty() + " ;AUTO_CONFIRMED").trim()
                            transactionDao.updateTransaction(toConfirm.copy(notes = nextNotes))
                        }
                    },
                    onEdit = { editingTxn = it }
                )
            }
        }
    }

    editingTxn?.let { txn ->
        EditLedgerDialog(
            transaction = txn,
            onDismiss = { editingTxn = null },
            onSave = { updated ->
                scope.launch(Dispatchers.IO) { transactionDao.updateTransaction(updated) }
                editingTxn = null
            }
        )
    }
}

@Composable
private fun MonthHeader(monthText: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrev) { Text("<") }
            Text(monthText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onNext) { Text(">") }
        }
    }
}

@Composable
private fun SummaryCard(expense: Double, income: Double, total: Double) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Expense Rs ${money(expense)}", color = MaterialTheme.colorScheme.error)
            Text("Income Rs ${money(income)}", color = Color(0xFF1B8B4B))
            Text("Total Rs ${money(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmartCards(
    safeSpend: Double,
    topCategory: String?,
    upcomingPlans: List<FinancialPlanEntry>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Daily Safe Spend", fontWeight = FontWeight.SemiBold)
                Text("Safe to spend today Rs ${money(safeSpend)}", color = MaterialTheme.colorScheme.primary)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Upcoming Bills", fontWeight = FontWeight.SemiBold)
                val nextBill = upcomingPlans.firstOrNull { !it.isCompleted }
                if (nextBill == null) {
                    Text("No pending reminders")
                } else {
                    Text("${nextBill.title}  Rs ${money(nextBill.amount)}")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Smart Insights", fontWeight = FontWeight.SemiBold)
                val insight = if (topCategory == null) {
                    "Add transactions to unlock spending insights."
                } else {
                    "Most spending this month is in $topCategory. Try capping this category for better control."
                }
                Text(insight)
            }
        }
    }
}

@Composable
private fun LedgerRow(
    transaction: Transaction,
    onConfirmAuto: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
) {
    val icon = categoryIcon(transaction.category)
    val isAutoDetected = !transaction.isManualEntry
    val confirmed = transaction.notes.orEmpty().contains("AUTO_CONFIRMED")

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(categoryColor(transaction.category)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.merchantName ?: transaction.category, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${transaction.category} • ${resolveRail(transaction)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Rs ${money(abs(transaction.amount))}",
                    fontWeight = FontWeight.Bold,
                    color = if (TransactionClassifier.isCredit(transaction)) Color(0xFF1B8B4B) else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isAutoDetected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = {}, label = { Text("?? Auto detected") })
                    if (!confirmed) {
                        TextButton(onClick = { onConfirmAuto(transaction) }) { Text("Confirm") }
                    }
                    TextButton(onClick = { onEdit(transaction) }) { Text("Edit") }
                }
            }
        }
    }
}

@Composable
private fun EditLedgerDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var merchant by remember(transaction.id) { mutableStateOf(transaction.merchantName ?: "") }
    var category by remember(transaction.id) { mutableStateOf(transaction.category) }
    var amount by remember(transaction.id) { mutableStateOf(money(abs(transaction.amount))) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = merchant, onValueChange = { merchant = it.take(36) }, label = { Text("Merchant") })
                OutlinedTextField(value = category, onValueChange = { category = it.take(24) }, label = { Text("Category") })
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = amount.toDoubleOrNull() ?: return@TextButton
                onSave(
                    transaction.copy(
                        merchantName = merchant.trim().ifBlank { transaction.category },
                        category = category.trim().ifBlank { "Other" },
                        amount = parsed
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun resolveRail(txn: Transaction): String {
    val source = txn.rawSmsText.orEmpty().lowercase(Locale.getDefault())
    return when {
        source.contains("upi") -> "UPI"
        source.contains("card") -> "Card"
        txn.bankName?.contains("wallet", true) == true -> "Wallet"
        txn.bankName != null -> txn.bankName
        else -> "Cash"
    }
}

private fun categoryIcon(category: String): String {
    val normalized = category.lowercase(Locale.getDefault())
    return when {
        normalized.contains("food") -> "??"
        normalized.contains("transport") -> "??"
        normalized.contains("bill") -> "??"
        normalized.contains("shop") -> "??"
        normalized.contains("health") -> "??"
        normalized.contains("entertain") -> "??"
        else -> "??"
    }
}

private fun categoryColor(category: String): Color {
    val normalized = category.lowercase(Locale.getDefault())
    return when {
        normalized.contains("food") -> Color(0xFFFFE6C7)
        normalized.contains("transport") -> Color(0xFFDDF2FF)
        normalized.contains("bill") -> Color(0xFFFFF1CC)
        normalized.contains("shop") -> Color(0xFFFFE0F1)
        normalized.contains("health") -> Color(0xFFE3F7E3)
        normalized.contains("entertain") -> Color(0xFFE8E3FF)
        else -> Color(0xFFE9EDF4)
    }
}

private fun money(value: Double): String = String.format("%.0f", value)
