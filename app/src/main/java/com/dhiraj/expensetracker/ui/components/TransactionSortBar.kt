package com.dhiraj.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransactionSortBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(onClick = {}, label = { Text("Merchant") })
        AssistChip(onClick = {}, label = { Text("Category") })
        AssistChip(onClick = {}, label = { Text("Date") })
    }
}
