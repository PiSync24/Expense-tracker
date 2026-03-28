package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
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
import kotlin.math.abs

private data class AccountBalance(val name: String, val amount: Double)

@Composable
fun AccountsScreen() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val dao = remember(db) { db.transactionDao() }
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    val accountBalances = remember(transactions) {
        transactions.groupBy { it.bankName ?: "Cash" }
            .map { (account, list) ->
                val income = list.filter { TransactionClassifier.isCredit(it) }.sumOf { abs(it.amount) }
                val expense = list.filterNot { TransactionClassifier.isCredit(it) }.sumOf { abs(it.amount) }
                AccountBalance(account, income - expense)
            }
            .sortedByDescending { it.amount }
    }

    val autoDetected = remember(transactions) { transactions.filterNot { it.isManualEntry }.take(5) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Ledger balances across accounts")
        }

        accountBalances.forEach { account ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(account.name, fontWeight = FontWeight.Medium)
                        Text(
                            "Rs ${money(account.amount)}",
                            color = if (account.amount >= 0) Color(0xFF228B4E) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Auto-detected activity", fontWeight = FontWeight.SemiBold)
                    if (autoDetected.isEmpty()) {
                        Text("No auto-detected transactions yet")
                    } else {
                        autoDetected.forEach { tx ->
                            Text("?? ${tx.merchantName ?: tx.category} • Rs ${money(abs(tx.amount))}")
                        }
                    }
                }
            }
        }
    }
}

private fun money(value: Double): String = String.format("%.0f", value)
