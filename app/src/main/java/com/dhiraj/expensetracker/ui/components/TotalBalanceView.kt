package com.dhiraj.expensetracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.ui.utils.TransactionClassifier
import kotlin.math.abs

@Composable
fun TotalBalanceView() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val dao = remember(db) { db.transactionDao() }

    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    val balance = transactions.sumOf { tx ->
        if (TransactionClassifier.isCredit(tx)) {
            abs(tx.amount)
        } else {
            -abs(tx.amount)
        }
    }

    val color = if (balance >= 0) Color(0xFF2E8B57) else Color(0xFFB85C5C)

    Text(
        text = "₹${String.format("%.2f", balance)}",
        style = MaterialTheme.typography.titleMedium,
        color = color
    )
}
