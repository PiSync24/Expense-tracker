package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.ui.utils.TransactionClassifier
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

private data class BudgetItem(val category: String, val spent: Double, val limit: Double)

@Composable
fun BudgetsScreen() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val dao = remember(db) { db.transactionDao() }
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    val month = YearMonth.now()
    val currentMonthExpenses = remember(transactions) {
        transactions.filter {
            val date = it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            date.year == month.year && date.monthValue == month.monthValue && !TransactionClassifier.isCredit(it)
        }
    }

    val defaultLimits = mapOf(
        "Food" to 5000.0,
        "Transport" to 3000.0,
        "Bills" to 6000.0,
        "Shopping" to 4500.0,
        "Health" to 3500.0,
        "Entertainment" to 2500.0
    )

    val budgets = remember(currentMonthExpenses) {
        defaultLimits.map { (category, limit) ->
            val spent = currentMonthExpenses
                .filter { it.category.equals(category, ignoreCase = true) }
                .sumOf { abs(it.amount) }
            BudgetItem(category, spent, limit)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Budgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${month.year}")
        }

        budgets.forEach { item ->
            item {
                val progress = (item.spent / item.limit).toFloat().coerceAtLeast(0f)
                val exceeded = progress > 1f
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.category, fontWeight = FontWeight.SemiBold)
                        Text("Rs ${money(item.spent)} / Rs ${money(item.limit)}")
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (exceeded) Color(0xFFD93A3A) else Color(0xFF2A9D57)
                        )
                        if (exceeded) {
                            Text("Budget exceeded", color = Color(0xFFD93A3A), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun money(value: Double): String = String.format("%.0f", value)
