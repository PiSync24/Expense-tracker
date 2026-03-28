package com.dhiraj.expensetracker.ui.theme

import android.content.Context

object ThemePreferences {
    private const val PREFS_NAME = "expense_tracker_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
}
