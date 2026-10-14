package com.example.expensetracker.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.AppTheme
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExchangeRateRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val baseCurrency: String = "INR",
    val theme: AppTheme = AppTheme.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val lastSyncTime: Long = 0L,
    val isSyncingRates: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val showSeedDataDialog: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val appDatabase: AppDatabase? = null,
    private val context: Context? = null
) : ViewModel() {

    private val _isSyncingRates = MutableStateFlow(false)
    private val _showResetConfirmDialog = MutableStateFlow(false)
    private val _showSeedDataDialog = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.baseCurrency,
        userPreferencesRepository.theme,
        userPreferencesRepository.notificationsEnabled,
        userPreferencesRepository.hapticFeedbackEnabled,
        userPreferencesRepository.lastExchangeRateSyncTime,
        _isSyncingRates,
        _showResetConfirmDialog,
        _showSeedDataDialog,
        _message
    ) { args: Array<Any?> ->
        SettingsUiState(
            baseCurrency = args[0] as String,
            theme = args[1] as AppTheme,
            notificationsEnabled = args[2] as Boolean,
            hapticFeedbackEnabled = args[3] as Boolean,
            lastSyncTime = args[4] as Long,
            isSyncingRates = args[5] as Boolean,
            showResetConfirmDialog = args[6] as Boolean,
            showSeedDataDialog = args[7] as Boolean,
            message = args[8] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onBaseCurrencySelected(currencyCode: String) {
        val normalizedCode = currencyCode.uppercase().trim()
        userPreferencesRepository.setBaseCurrency(normalizedCode)
        syncExchangeRates(normalizedCode)
    }

    fun onThemeSelected(theme: AppTheme) {
        userPreferencesRepository.setTheme(theme)
    }

    fun onNotificationsToggled(enabled: Boolean) {
        userPreferencesRepository.setNotificationsEnabled(enabled)
    }

    fun onHapticFeedbackToggled(enabled: Boolean) {
        userPreferencesRepository.setHapticFeedbackEnabled(enabled)
    }

    fun syncExchangeRates(baseCurr: String = userPreferencesRepository.baseCurrency.value) {
        viewModelScope.launch {
            val normalizedBase = baseCurr.uppercase().trim()
            _isSyncingRates.value = true
            val success = exchangeRateRepository.refreshRates(normalizedBase)
            transactionRepository.recalculateConvertedAmounts(normalizedBase)
            _isSyncingRates.value = false
            if (success) {
                userPreferencesRepository.setLastExchangeRateSyncTime(System.currentTimeMillis())
                _message.value = "Exchange rates updated successfully"
            } else {
                _message.value = "Failed to update rates. Using cached rates."
            }
        }
    }

    fun requestResetData() {
        _showResetConfirmDialog.value = true
    }

    fun dismissResetDataDialog() {
        _showResetConfirmDialog.value = false
    }

    fun confirmResetData() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Wipe all transactions and budgets from local database
            transactionRepository.clearAllTransactions()
            budgetRepository.clearAllBudgets()

            // 2. Clear cached exchange rates
            exchangeRateRepository.clearCachedRates()

            // 3. Reset custom categories back to default categories
            try {
                appDatabase?.let { db ->
                    val sqliteDb = db.openHelper.writableDatabase
                    sqliteDb.execSQL("DELETE FROM categories WHERE isDefault = 0")
                    AppDatabase.DEFAULT_CATEGORIES.forEach { cat ->
                        sqliteDb.execSQL(
                            "INSERT OR IGNORE INTO categories (id, name, type, icon, isDefault, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf(cat.id, cat.name, cat.type, cat.icon, if (cat.isDefault) 1 else 0, cat.createdAt)
                        )
                    }
                }
            } catch (_: Exception) {}

            // 4. Checkpoint WAL log and execute VACUUM to shrink database storage on phone filesystem
            appDatabase?.vacuumAndCheckpoint()

            // 5. Clear application cache directory
            try {
                context?.let { ctx ->
                    ctx.cacheDir.deleteRecursively()
                    ctx.codeCacheDir.deleteRecursively()
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                _showResetConfirmDialog.value = false
                _message.value = "All application data permanently erased and storage compacted."
            }
        }
    }

    fun requestSeedSampleData() {
        _showSeedDataDialog.value = true
    }

    fun dismissSeedDataDialog() {
        _showSeedDataDialog.value = false
    }

    fun confirmSeedSampleData() {
        viewModelScope.launch {
            val baseCurr = userPreferencesRepository.baseCurrency.value
            val now = System.currentTimeMillis()

            // Ensure categories exist
            categoryRepository.allCategories.collect { categories ->
                if (categories.isNotEmpty()) {
                    val salaryCat = categories.find { it.name.contains("Salary", ignoreCase = true) }?.id ?: 12L
                    val foodCat = categories.find { it.name.contains("Food", ignoreCase = true) }?.id ?: 1L
                    val transCat = categories.find { it.name.contains("Transportation", ignoreCase = true) }?.id ?: 2L
                    val utilCat = categories.find { it.name.contains("Utilities", ignoreCase = true) }?.id ?: 4L
                    val entCat = categories.find { it.name.contains("Entertainment", ignoreCase = true) }?.id ?: 5L

                    // Sample Income
                    transactionRepository.addTransaction(
                        type = "INCOME",
                        amount = 4500.0,
                        currency = baseCurr,
                        baseCurrency = baseCurr,
                        categoryId = salaryCat,
                        date = now - (2 * 24 * 3600 * 1000L),
                        note = "Monthly Salary Deposit",
                        paymentMethod = "Bank Transfer"
                    )

                    // Sample Expenses
                    transactionRepository.addTransaction(
                        type = "EXPENSE",
                        amount = 120.50,
                        currency = baseCurr,
                        baseCurrency = baseCurr,
                        categoryId = foodCat,
                        date = now - (1 * 24 * 3600 * 1000L),
                        note = "Weekly Grocery Shopping",
                        paymentMethod = "Credit Card"
                    )

                    transactionRepository.addTransaction(
                        type = "EXPENSE",
                        amount = 45.00,
                        currency = baseCurr,
                        baseCurrency = baseCurr,
                        categoryId = transCat,
                        date = now - (3 * 24 * 3600 * 1000L),
                        note = "Fuel Fill Up",
                        paymentMethod = "Debit Card"
                    )

                    transactionRepository.addTransaction(
                        type = "EXPENSE",
                        amount = 180.00,
                        currency = baseCurr,
                        baseCurrency = baseCurr,
                        categoryId = utilCat,
                        date = now - (5 * 24 * 3600 * 1000L),
                        note = "Electricity & Internet Bill",
                        paymentMethod = "Bank Transfer"
                    )

                    transactionRepository.addTransaction(
                        type = "EXPENSE",
                        amount = 35.00,
                        currency = "EUR",
                        baseCurrency = baseCurr,
                        categoryId = entCat,
                        date = now - (6 * 24 * 3600 * 1000L),
                        note = "Movie Night (Converted from EUR)",
                        paymentMethod = "Cash"
                    )

                    _showSeedDataDialog.value = false
                    _message.value = "Sample dataset seeded successfully!"
                    return@collect
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val exchangeRateRepository: ExchangeRateRepository,
        private val transactionRepository: TransactionRepository,
        private val budgetRepository: BudgetRepository,
        private val categoryRepository: CategoryRepository,
        private val appDatabase: AppDatabase? = null,
        private val context: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                userPreferencesRepository,
                exchangeRateRepository,
                transactionRepository,
                budgetRepository,
                categoryRepository,
                appDatabase,
                context
            ) as T
        }
    }
}
