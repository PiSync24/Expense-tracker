package com.dhiraj.expensetracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.ui.navigation.BottomNavItem
import com.dhiraj.expensetracker.ui.screens.AccountsScreen
import com.dhiraj.expensetracker.ui.screens.AddTransactionScreen
import com.dhiraj.expensetracker.ui.screens.AnalysisScreen
import com.dhiraj.expensetracker.ui.screens.BudgetsScreen
import com.dhiraj.expensetracker.ui.screens.CategoriesScreen
import com.dhiraj.expensetracker.ui.screens.TransactionsScreen
import com.dhiraj.expensetracker.ui.theme.AppPalette

private const val ADD_TRANSACTION_ROUTE = "add_transaction"

@Suppress("UNUSED_PARAMETER")
@Composable
fun MainScaffold(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    selectedPalette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val transactionDao = remember(db) { db.transactionDao() }

    val navController = rememberNavController()

    val currentRoute by navController
        .currentBackStackEntryAsState()
        .let { derivedStateOf { it.value?.destination?.route ?: BottomNavItem.Records.route } }

    val bottomItems = listOf(
        BottomNavItem.Records,
        BottomNavItem.Analysis,
        BottomNavItem.Budgets,
        BottomNavItem.Accounts,
        BottomNavItem.Categories
    )

    val showBottomNav = bottomItems.any { it.route == currentRoute }

    Scaffold(
        floatingActionButton = {
            if (showBottomNav) {
                FloatingActionButton(onClick = { navController.navigate(ADD_TRANSACTION_ROUTE) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        },
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Records.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Records.route) { TransactionsScreen() }
            composable(BottomNavItem.Analysis.route) { AnalysisScreen() }
            composable(BottomNavItem.Budgets.route) { BudgetsScreen() }
            composable(BottomNavItem.Accounts.route) { AccountsScreen() }
            composable(BottomNavItem.Categories.route) { CategoriesScreen() }
            composable(ADD_TRANSACTION_ROUTE) {
                AddTransactionScreen(
                    transactionDao = transactionDao,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
