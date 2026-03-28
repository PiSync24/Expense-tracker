package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.data.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs

private enum class EntryType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer")
}

private val keypad = listOf(
    "7", "8", "9", "+",
    "4", "5", "6", "-",
    "1", "2", "3", "*",
    "0", ".", "=", "/"
)

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionDao: TransactionDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val categoryDao = remember(db) { db.categoryDao() }

    val categories by categoryDao.getAllCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var entryType by remember { mutableStateOf(EntryType.EXPENSE) }
    var selectedAccount by remember { mutableStateOf("Cash") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var notes by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("0") }

    val accountOptions = listOf("Cash", "HDFC Bank", "ICICI Bank", "UPI Wallet", "Credit Card")
    val categoryOptions = if (categories.isEmpty()) {
        listOf("Food", "Transport", "Bills", "Shopping", "Health", "Entertainment")
    } else {
        categories.map { it.name }.distinct()
    }

    val displayAmount = evaluateExpression(expression)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Add Transaction", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Calculator style entry", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SelectorCard(title = "Type") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    EntryType.entries.forEach { type ->
                        FilterChip(
                            selected = entryType == type,
                            onClick = { entryType = type },
                            label = { Text(type.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            SelectorCard(title = "Account") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accountOptions.forEach { account ->
                        FilterChip(
                            selected = selectedAccount == account,
                            onClick = { selectedAccount = account },
                            label = { Text(account) }
                        )
                    }
                }
            }
        }

        item {
            SelectorCard(title = "Category") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryOptions.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(60) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text(expression, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Rs ${formatMoney(displayAmount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(280.dp),
                userScrollEnabled = false
            ) {
                items(keypad) { key ->
                    Button(
                        onClick = {
                            expression = if (key == "=") {
                                formatMoney(evaluateExpression(expression))
                            } else {
                                appendKey(expression, key)
                            }
                        },
                        modifier = Modifier.height(58.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(key, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onBack() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = {
                        if (displayAmount <= 0.0) return@Button
                        val rawType = when (entryType) {
                            EntryType.INCOME -> "MANUAL_INCOME"
                            EntryType.EXPENSE -> "MANUAL_EXPENSE"
                            EntryType.TRANSFER -> "MANUAL_TRANSFER"
                        }
                        scope.launch(Dispatchers.IO) {
                            transactionDao.insertTransaction(
                                Transaction(
                                    amount = abs(displayAmount),
                                    merchantName = selectedCategory,
                                    category = selectedCategory,
                                    date = Date(),
                                    bankName = selectedAccount,
                                    upiId = null,
                                    rawSmsText = rawType,
                                    isManualEntry = true,
                                    notes = "Account:$selectedAccount;${notes.trim()}"
                                )
                            )
                        }
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SelectorCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

private fun appendKey(current: String, key: String): String {
    val numeric = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    val sanitized = if (current == "0" && key in numeric) "" else current
    return (sanitized + key)
}

private fun evaluateExpression(expression: String): Double {
    val tokens = expression
    return runCatching {
        val parts = mutableListOf<String>()
        var current = ""
        tokens.forEach { ch ->
            if (ch in listOf('+', '-', '*', '/')) {
                if (current.isNotBlank()) parts += current
                parts += ch.toString()
                current = ""
            } else {
                current += ch
            }
        }
        if (current.isNotBlank()) parts += current

        if (parts.isEmpty()) return 0.0

        val values = mutableListOf<Double>()
        val ops = mutableListOf<String>()

        fun applyLast() {
            if (values.size < 2 || ops.isEmpty()) return
            val b = values.removeAt(values.lastIndex)
            val a = values.removeAt(values.lastIndex)
            val op = ops.removeAt(ops.lastIndex)
            values += when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b == 0.0) a else a / b
                else -> a
            }
        }

        parts.forEach { token ->
            when (token) {
                "*", "/" -> {
                    while (ops.lastOrNull() in listOf("*", "/")) applyLast()
                    ops += token
                }
                "+", "-" -> {
                    while (ops.isNotEmpty()) applyLast()
                    ops += token
                }
                else -> values += token.toDoubleOrNull() ?: 0.0
            }
        }

        while (ops.isNotEmpty()) applyLast()
        values.firstOrNull() ?: 0.0
    }.getOrDefault(0.0)
}

private fun formatMoney(value: Double): String = String.format("%.0f", value)
