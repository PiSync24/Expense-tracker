package com.dhiraj.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * PermissionWarningBanner
 *
 * A lightweight, non-blocking warning shown at the top of the app
 * when required permissions are missing.
 *
 * WHEN THIS IS SHOWN:
 * ------------------------------------------------
 * - App is running
 * - Notification permission OR notification access is missing
 * - User dismissed the permission dialog
 *
 * WHY THIS EXISTS:
 * ------------------------------------------------
 * - We do NOT want to block the UI
 * - We still want to inform the user that app is in limited mode
 *
 * WHAT THIS DOES NOT DO:
 * ------------------------------------------------
 * ❌ Does not request permission
 * ❌ Does not open settings
 * ❌ Does not interrupt navigation
 */
@Composable
fun PermissionWarningBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "⚠ Permissions missing. Transaction tracking may not work.",
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
