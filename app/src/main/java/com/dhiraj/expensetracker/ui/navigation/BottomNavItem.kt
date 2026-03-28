package com.dhiraj.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Records : BottomNavItem("records", "Records", Icons.Filled.List)
    object Analysis : BottomNavItem("analysis", "Analysis", Icons.Filled.ShowChart)
    object Budgets : BottomNavItem("budgets", "Budgets", Icons.Filled.PieChart)
    object Accounts : BottomNavItem("accounts", "Accounts", Icons.Filled.AccountBalance)
    object Categories : BottomNavItem("categories", "Categories", Icons.Filled.Category)
}
