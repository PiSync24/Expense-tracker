package com.dhiraj.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme


@Composable
fun TransactionFilterDropdown(
    onMerchant: () -> Unit,
    onCategory: () -> Unit,
    onDate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Filter & Sort")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Merchant") },
                onClick = {
                    expanded = false
                    onMerchant()
                }
            )
            DropdownMenuItem(
                text = { Text("Category") },
                onClick = {
                    expanded = false
                    onCategory()
                }
            )
            DropdownMenuItem(
                text = { Text("Date") },
                onClick = {
                    expanded = false
                    onDate()
                }
            )
        }
    }
}
