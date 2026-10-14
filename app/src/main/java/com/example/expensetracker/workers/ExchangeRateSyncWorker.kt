package com.example.expensetracker.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensetracker.ExpenseTrackerApp
import java.util.concurrent.TimeUnit

class ExchangeRateSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ExpenseTrackerApp ?: return Result.failure()
        return try {
            val lastSync = app.userPreferencesRepository.lastExchangeRateSyncTime.value
            val now = System.currentTimeMillis()
            // If already synced within last 12 hours, complete immediately with zero network/disk overhead
            if (now - lastSync < TimeUnit.HOURS.toMillis(12)) {
                return Result.success()
            }

            val baseCurrency = app.userPreferencesRepository.baseCurrency.value
            val success = app.exchangeRateRepository.refreshRates(baseCurrency)

            if (success) {
                app.transactionRepository.recalculateConvertedAmounts(baseCurrency)
                app.userPreferencesRepository.setLastExchangeRateSyncTime(System.currentTimeMillis())
            }
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "PeriodicExchangeRateSyncWork"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            val workManager = WorkManager.getInstance(context)
            // Cancel legacy 24h work if it was running under old name
            workManager.cancelUniqueWork("DailyExchangeRateSyncWork")

            // Use ExistingPeriodicWorkPolicy.KEEP so it does NOT restart / re-run on every cold launch
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun scheduleDailySync(context: Context) = schedulePeriodicSync(context)
    }
}
