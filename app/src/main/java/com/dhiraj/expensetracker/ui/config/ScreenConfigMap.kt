package com.dhiraj.expensetracker.ui.config

import com.dhiraj.expensetracker.ui.navigation.BottomNavItem

val screenConfigMap = mapOf(
    BottomNavItem.Records.route to ScreenConfig("Records", true),
    BottomNavItem.Analysis.route to ScreenConfig("Analysis", false),
    BottomNavItem.Budgets.route to ScreenConfig("Budgets", false),
    BottomNavItem.Accounts.route to ScreenConfig("Accounts", false),
    BottomNavItem.Categories.route to ScreenConfig("Categories", false)
)
