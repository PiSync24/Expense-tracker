package com.dhiraj.expensetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.ui.utils.TransactionClassifier
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    txn: Transaction,
    categoryEmoji: String,
    categoryColor: Color,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate = remember(txn.date) { formatDate(txn.date) }
    val isCredit = TransactionClassifier.isCredit(txn)
    val amountColor = if (isCredit) Color(0xFF2E8B57) else Color(0xFFB85C5C)

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showMenu = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                width = 1.dp,
                color = categoryColor.copy(alpha = 0.22f)
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.20f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = categoryEmoji)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = txn.merchantName ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "₹${String.format("%.2f", abs(txn.amount))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$categoryEmoji ${txn.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = categoryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit(txn)
                }
            )

            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete(txn)
                }
            )
        }
    }
}

private val rowDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

private fun formatDate(date: Date): String {
    return date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(rowDateFormatter)
}
