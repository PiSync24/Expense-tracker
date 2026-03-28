package com.dhiraj.expensetracker

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.dhiraj.expensetracker.notifications.BankNotificationListener
import com.dhiraj.expensetracker.notifications.TransactionNotificationManager
import com.dhiraj.expensetracker.ui.MainScaffold
import com.dhiraj.expensetracker.ui.components.NotificationAccessDialog
import com.dhiraj.expensetracker.ui.components.PermissionWarningBanner
import com.dhiraj.expensetracker.ui.theme.AppPreferences
import com.dhiraj.expensetracker.ui.theme.ExpenseTrackerTheme
import com.dhiraj.expensetracker.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val permissionStateFlow = MutableStateFlow(false)

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TransactionNotificationManager.createNotificationChannel(this)
        permissionStateFlow.value = areAllRequiredPermissionsGranted()
        requestPostNotificationPermissionIfNeeded()

        val appContext = applicationContext
        AppPreferences.ensureInitialized(appContext)

        setContent {
            val hasPermissions by permissionStateFlow.collectAsState()
            var isDarkTheme by rememberSaveable {
                mutableStateOf(ThemePreferences.isDarkModeEnabled(appContext))
            }
            var selectedPalette by rememberSaveable {
                mutableStateOf(AppPreferences.getPalette(appContext))
            }

            var showPermissionDialog by remember {
                mutableStateOf(!hasPermissions)
            }

            ExpenseTrackerTheme(
                darkTheme = isDarkTheme,
                palette = selectedPalette,
                dynamicColor = false
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScaffold(
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { enabled ->
                            isDarkTheme = enabled
                            ThemePreferences.setDarkModeEnabled(appContext, enabled)
                        },
                        selectedPalette = selectedPalette,
                        onPaletteChange = { palette ->
                            selectedPalette = palette
                            AppPreferences.setPalette(appContext, palette)
                        }
                    )

                    if (!hasPermissions && !showPermissionDialog) {
                        PermissionWarningBanner()
                    }

                    if (!hasPermissions && showPermissionDialog) {
                        NotificationAccessDialog(
                            onEnable = {
                                showPermissionDialog = false
                                openNotificationAccessSettings()
                            },
                            onDismiss = {
                                showPermissionDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun areAllRequiredPermissionsGranted(): Boolean {
        val postNotificationGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        val notificationAccessGranted = isNotificationAccessGranted()

        Log.d(
            "PERMISSION_STATE",
            "POST_NOTIFICATIONS=$postNotificationGranted, " +
                "NOTIFICATION_ACCESS=$notificationAccessGranted"
        )

        return postNotificationGranted && notificationAccessGranted
    }

    private fun requestPostNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

        val componentName = ComponentName(
            this,
            BankNotificationListener::class.java
        ).flattenToString()

        return enabledListeners.contains(componentName)
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_HOVER_EXIT) {
            return true
        }

        return runCatching { super.dispatchGenericMotionEvent(ev) }
            .getOrElse { throwable ->
                if (
                    throwable is IllegalStateException &&
                    throwable.message?.contains("ACTION_HOVER_EXIT") == true
                ) {
                    true
                } else {
                    throw throwable
                }
            }
    }

    private fun refreshPermissionState() {
        permissionStateFlow.value = areAllRequiredPermissionsGranted()
    }
}
