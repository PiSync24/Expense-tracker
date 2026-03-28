package com.dhiraj.expensetracker.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    showBalance: Boolean,
    onSettingsClick: () -> Unit,
    onCategoriesClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        actions = {
            if (showBalance) {
                TotalBalanceView()
            }
        }
    )
}
