package com.dhiraj.expensetracker.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppPreferences {
    private const val PREFS_NAME = "expense_tracker_prefs"

    private const val KEY_SMART_CATEGORY = "smart_category_enabled"
    private const val KEY_AUTO_SETTLE_LOANS = "auto_settle_loans"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_APP_PALETTE = "app_palette"
    private const val KEY_SELECTED_TEMPLATE_ID = "selected_template_id"
    private const val KEY_MONTHLY_INCOME_PREFIX = "monthly_income_template_"

    @Volatile
    private var initialized = false
    private lateinit var preferences: SharedPreferences

    private val monthlyIncomeFlow = MutableStateFlow(0.0)
    private val paletteFlow = MutableStateFlow(AppPalette.CALM)
    private val selectedTemplateIdFlow = MutableStateFlow(1L)

    fun ensureInitialized(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            preferences = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            selectedTemplateIdFlow.value =
                preferences.getLong(KEY_SELECTED_TEMPLATE_ID, 1L).coerceAtLeast(1L)
            monthlyIncomeFlow.value = readMonthlyIncomeForTemplate(selectedTemplateIdFlow.value)
            paletteFlow.value = readPalette()

            initialized = true
        }
    }

    fun isSmartCategoryEnabled(context: Context): Boolean {
        ensureInitialized(context)
        return preferences.getBoolean(KEY_SMART_CATEGORY, true)
    }

    fun setSmartCategoryEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        preferences.edit().putBoolean(KEY_SMART_CATEGORY, enabled).apply()
    }

    fun isAutoSettleLoansEnabled(context: Context): Boolean {
        ensureInitialized(context)
        return preferences.getBoolean(KEY_AUTO_SETTLE_LOANS, true)
    }

    fun setAutoSettleLoansEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        preferences.edit().putBoolean(KEY_AUTO_SETTLE_LOANS, enabled).apply()
    }

    fun isReduceMotionEnabled(context: Context): Boolean {
        ensureInitialized(context)
        return preferences.getBoolean(KEY_REDUCE_MOTION, false)
    }

    fun setReduceMotionEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        preferences.edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()
    }

    fun monthlyIncomeState(context: Context): StateFlow<Double> {
        ensureInitialized(context)
        return monthlyIncomeFlow
    }

    fun getMonthlyIncome(context: Context): Double {
        ensureInitialized(context)
        return readMonthlyIncomeForTemplate(selectedTemplateIdFlow.value)
    }

    fun getMonthlyIncome(context: Context, templateId: Long): Double {
        ensureInitialized(context)
        return readMonthlyIncomeForTemplate(templateId)
    }

    fun setMonthlyIncome(context: Context, income: Double) {
        ensureInitialized(context)
        setMonthlyIncome(context, selectedTemplateIdFlow.value, income)
    }

    fun setMonthlyIncome(context: Context, templateId: Long, income: Double) {
        ensureInitialized(context)
        val safeTemplate = templateId.coerceAtLeast(1L)
        writeDouble(keyForTemplateIncome(safeTemplate), income)
        if (safeTemplate == selectedTemplateIdFlow.value) {
            monthlyIncomeFlow.value = income
        }
    }

    fun paletteState(context: Context): StateFlow<AppPalette> {
        ensureInitialized(context)
        return paletteFlow
    }

    fun getPalette(context: Context): AppPalette {
        ensureInitialized(context)
        return paletteFlow.value
    }

    fun setPalette(context: Context, palette: AppPalette) {
        ensureInitialized(context)
        preferences.edit().putString(KEY_APP_PALETTE, palette.name).apply()
        paletteFlow.value = palette
    }

    fun selectedTemplateIdState(context: Context): StateFlow<Long> {
        ensureInitialized(context)
        return selectedTemplateIdFlow
    }

    fun getSelectedTemplateId(context: Context): Long {
        ensureInitialized(context)
        return selectedTemplateIdFlow.value
    }

    fun setSelectedTemplateId(context: Context, templateId: Long) {
        ensureInitialized(context)
        val safeId = templateId.coerceAtLeast(1L)
        preferences.edit().putLong(KEY_SELECTED_TEMPLATE_ID, safeId).apply()
        selectedTemplateIdFlow.value = safeId
        monthlyIncomeFlow.value = readMonthlyIncomeForTemplate(safeId)
    }

    private fun keyForTemplateIncome(templateId: Long): String {
        return KEY_MONTHLY_INCOME_PREFIX + templateId
    }

    private fun readMonthlyIncomeForTemplate(templateId: Long): Double {
        return readDouble(keyForTemplateIncome(templateId.coerceAtLeast(1L)), 0.0)
    }

    private fun writeDouble(key: String, value: Double) {
        preferences.edit()
            .putLong(key, java.lang.Double.doubleToRawLongBits(value))
            .apply()
    }

    private fun readDouble(key: String, default: Double): Double {
        return java.lang.Double.longBitsToDouble(
            preferences.getLong(key, java.lang.Double.doubleToRawLongBits(default))
        )
    }

    private fun readPalette(): AppPalette {
        val stored = preferences.getString(KEY_APP_PALETTE, AppPalette.CALM.name)
        return runCatching { AppPalette.valueOf(stored ?: AppPalette.CALM.name) }
            .getOrElse { AppPalette.CALM }
    }
}
