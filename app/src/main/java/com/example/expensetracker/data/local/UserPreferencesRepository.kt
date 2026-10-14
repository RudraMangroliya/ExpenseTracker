package com.example.expensetracker.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _baseCurrency = MutableStateFlow(prefs.getString("base_currency", "INR") ?: "INR")
    val baseCurrency: StateFlow<String> = _baseCurrency.asStateFlow()

    private val initialTheme = try {
        AppTheme.valueOf(prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)
    } catch (e: Exception) {
        AppTheme.SYSTEM
    }

    private val _theme = MutableStateFlow(initialTheme)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean("notifications_enabled", true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _hapticFeedbackEnabled = MutableStateFlow(
        prefs.getBoolean("haptic_feedback_enabled", true)
    )
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()

    private val _lastExchangeRateSyncTime = MutableStateFlow(
        prefs.getLong("last_exchange_rate_sync_time", 0L)
    )
    val lastExchangeRateSyncTime: StateFlow<Long> = _lastExchangeRateSyncTime.asStateFlow()

    fun setBaseCurrency(currencyCode: String) {
        prefs.edit().putString("base_currency", currencyCode).apply()
        _baseCurrency.value = currencyCode
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _theme.value = theme
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
        _hapticFeedbackEnabled.value = enabled
    }

    fun setLastExchangeRateSyncTime(timestamp: Long) {
        prefs.edit().putLong("last_exchange_rate_sync_time", timestamp).apply()
        _lastExchangeRateSyncTime.value = timestamp
    }

    private val cal = java.util.Calendar.getInstance()
    private val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
    private val currentYear = cal.get(java.util.Calendar.YEAR)

    private val savedBudgetMonth = prefs.getInt("cached_budget_month", -1)
    private val savedBudgetYear = prefs.getInt("cached_budget_year", -1)
    private val initialMonthlyBudget = if (savedBudgetMonth == currentMonth && savedBudgetYear == currentYear) {
        prefs.getFloat("cached_monthly_budget", 0f).toDouble()
    } else {
        0.0
    }
    private val initialMonthlyExpense = if (savedBudgetMonth == currentMonth && savedBudgetYear == currentYear) {
        prefs.getFloat("cached_monthly_expense", 0f).toDouble()
    } else {
        0.0
    }

    private val _cachedMonthlyBudget = MutableStateFlow(initialMonthlyBudget)
    val cachedMonthlyBudget: StateFlow<Double> = _cachedMonthlyBudget.asStateFlow()

    private val _cachedMonthlyExpense = MutableStateFlow(initialMonthlyExpense)
    val cachedMonthlyExpense: StateFlow<Double> = _cachedMonthlyExpense.asStateFlow()

    private val _cachedBalance = MutableStateFlow(prefs.getFloat("cached_balance", 0f).toDouble())
    val cachedBalance: StateFlow<Double> = _cachedBalance.asStateFlow()

    private val _cachedIncome = MutableStateFlow(prefs.getFloat("cached_income", 0f).toDouble())
    val cachedIncome: StateFlow<Double> = _cachedIncome.asStateFlow()

    private val _cachedExpense = MutableStateFlow(prefs.getFloat("cached_total_expense", 0f).toDouble())
    val cachedExpense: StateFlow<Double> = _cachedExpense.asStateFlow()

    fun updateCachedDashboardData(
        balance: Double,
        income: Double,
        expense: Double,
        monthlyExpense: Double,
        monthlyBudget: Double,
        month: Int,
        year: Int
    ) {
        if (_cachedBalance.value == balance &&
            _cachedIncome.value == income &&
            _cachedExpense.value == expense &&
            _cachedMonthlyExpense.value == monthlyExpense &&
            _cachedMonthlyBudget.value == monthlyBudget
        ) {
            return
        }

        _cachedBalance.value = balance
        _cachedIncome.value = income
        _cachedExpense.value = expense
        _cachedMonthlyExpense.value = monthlyExpense
        _cachedMonthlyBudget.value = monthlyBudget

        prefs.edit()
            .putFloat("cached_balance", balance.toFloat())
            .putFloat("cached_income", income.toFloat())
            .putFloat("cached_total_expense", expense.toFloat())
            .putFloat("cached_monthly_expense", monthlyExpense.toFloat())
            .putFloat("cached_monthly_budget", monthlyBudget.toFloat())
            .putInt("cached_budget_month", month)
            .putInt("cached_budget_year", year)
            .apply()
    }

    companion object {
        val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "JPY", "CAD", "AUD")
    }
}
