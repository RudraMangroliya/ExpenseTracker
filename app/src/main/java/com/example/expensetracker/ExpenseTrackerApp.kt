package com.example.expensetracker

import android.app.Application
import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.remote.RetrofitClient
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExchangeRateRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // Initialize notification channel asynchronously so main thread cold launch returns in 0ms
            try {
                com.example.expensetracker.utils.NotificationHelper.initNotificationChannel(this@ExpenseTrackerApp)
            } catch (_: Exception) {}
            // Warm up Room DB instance on background thread to prevent UI thread disk stalls
            try {
                database.openHelper.readableDatabase
                database.categoryDao().getCategoryCount()
            } catch (_: Exception) {}
            // Defer periodic background sync scheduling so cold launch UI has 100% CPU and I/O priority
            kotlinx.coroutines.delay(10000)
            com.example.expensetracker.workers.ExchangeRateSyncWorker.schedulePeriodicSync(this@ExpenseTrackerApp)
        }
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

    val exchangeRateRepository by lazy {
        ExchangeRateRepository(
            apiServiceProvider = { RetrofitClient.apiService },
            exchangeRateDao = database.exchangeRateDao()
        )
    }

    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val budgetRepository by lazy { BudgetRepository(database.budgetDao()) }
    val transactionRepository by lazy {
        TransactionRepository(
            transactionDao = database.transactionDao(),
            exchangeRateRepository = exchangeRateRepository,
            appDatabase = database
        )
    }
}
