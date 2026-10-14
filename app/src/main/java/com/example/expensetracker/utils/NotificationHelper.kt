package com.example.expensetracker.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.max

object NotificationHelper {

    const val CHANNEL_ID = "budget_alerts_channel"
    private const val CHANNEL_NAME = "Budget & Spending Alerts"
    private const val CHANNEL_DESC = "Alerts and warnings when monthly or category spending approaches or exceeds budget limits"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "INR" -> "₹"
            "CAD" -> "CA$"
            "AUD" -> "AU$"
            else -> currencyCode
        }
    }

    /**
     * Generates a clean, circular badge bitmap for notifications featuring either
     * the Wallet icon (for monthly overall budget) or the Category icon.
     */
    fun getNotificationBadgeBitmap(
        context: Context,
        iconName: String?,
        isCritical: Boolean
    ): Bitmap? {
        return try {
            val drawableResId = when (iconName?.lowercase()) {
                "wallet" -> R.drawable.ic_notif_wallet
                "school", "education" -> R.drawable.ic_notif_school
                "fastfood", "food", "dining" -> R.drawable.ic_notif_fastfood
                "directions_car", "transportation", "car" -> R.drawable.ic_notif_directions_car
                "home", "housing", "rent" -> R.drawable.ic_notif_home
                "receipt_long", "bills", "utilities" -> R.drawable.ic_notif_receipt_long
                "movie", "entertainment" -> R.drawable.ic_notif_movie
                "shopping_bag", "shopping" -> R.drawable.ic_notif_shopping_bag
                "medical_services", "healthcare", "health" -> R.drawable.ic_notif_medical_services
                "flight", "travel" -> R.drawable.ic_notif_flight
                "spa", "personal", "personal care" -> R.drawable.ic_notif_spa
                "payments", "salary" -> R.drawable.ic_notif_payments
                "work", "freelance", "business" -> R.drawable.ic_notif_work
                "trending_up", "investments" -> R.drawable.ic_notif_trending_up
                "card_giftcard", "gifts", "allowance" -> R.drawable.ic_notif_card_giftcard
                "savings" -> R.drawable.ic_notif_savings
                null -> R.drawable.ic_notif_wallet
                else -> R.drawable.ic_notif_more
            }

            val density = context.resources.displayMetrics.density
            val sizePx = (56 * density).toInt().coerceAtLeast(168)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Circle Background Color - Elegant professional accent
            val bgColor = if (isCritical) {
                0xFFD32F2F.toInt() // Crimson Red for exceeded
            } else {
                0xFFF57C00.toInt() // Warm Amber for warning
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor
                style = Paint.Style.FILL
            }

            val radius = sizePx / 2f
            canvas.drawCircle(radius, radius, radius, paint)

            // Crisp subtle border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x2AFFFFFF.toInt() // 16% white outline
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
            }
            canvas.drawCircle(radius, radius, radius - (0.75f * density), borderPaint)

            // Draw the vector icon centered
            val drawable = ContextCompat.getDrawable(context, drawableResId)
            if (drawable != null) {
                val iconInset = (sizePx * 0.22f).toInt()
                drawable.setBounds(iconInset, iconInset, sizePx - iconInset, sizePx - iconInset)
                drawable.setTint(android.graphics.Color.WHITE)
                drawable.draw(canvas)
            }

            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun sendBudgetAlertNotification(
        context: Context,
        title: String,
        contentSummary: String,
        expandedDetails: String,
        progressPercent: Int = 0,
        isCritical: Boolean = false,
        notificationId: Int = 1001,
        iconName: String? = null
    ) {
        if (!hasNotificationPermission(context)) return

        initNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val badgeBitmap = getNotificationBadgeBitmap(context, iconName, isCritical)

        val accentColor = if (isCritical) {
            0xFFD32F2F.toInt() // Red for exceeded
        } else {
            0xFFF57C00.toInt() // Orange for warning
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(contentSummary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedDetails)
                    .setBigContentTitle(title)
                    .setSummaryText(if (isCritical) "Limit Exceeded" else "Spending Warning")
            )
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (badgeBitmap != null) {
            builder.setLargeIcon(badgeBitmap)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is missing or revoked
        }
    }

    suspend fun checkAndTriggerBudgetAlerts(
        context: Context,
        budgetRepository: BudgetRepository,
        transactionRepository: TransactionRepository,
        userPreferencesRepository: UserPreferencesRepository,
        categoryRepository: CategoryRepository,
        transactionDate: Long,
        categoryId: Long?
    ) {
        // 1. Check if user enabled budget notifications in Settings
        val notificationsEnabled = userPreferencesRepository.notificationsEnabled.first()
        if (!notificationsEnabled) return

        val baseCurrency = userPreferencesRepository.baseCurrency.first()
        val currSymbol = getCurrencySymbol(baseCurrency)

        val cal = Calendar.getInstance().apply { timeInMillis = transactionDate }
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        // Calculate Month Start & End
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startTimestamp = startCal.timeInMillis
        val endTimestamp = endCal.timeInMillis

        // Fetch monthly transactions
        val monthlyTransactions = transactionRepository.getTransactionsByDateRange(startTimestamp, endTimestamp).first()
        val totalExpense = monthlyTransactions.filter { it.transaction.type == "EXPENSE" }
            .sumOf { it.transaction.convertedAmount }

        // ==========================================
        // 1. Check Overall Monthly Budget
        // ==========================================
        val overallBudget = budgetRepository.getOverallBudget(month, year).first()
        if (overallBudget != null && overallBudget.amount > 0.0) {
            val budgetLimit = overallBudget.amount
            val percentage = (totalExpense / budgetLimit) * 100.0
            val remaining = max(0.0, budgetLimit - totalExpense)

            if (totalExpense >= budgetLimit) {
                val overage = totalExpense - budgetLimit
                sendBudgetAlertNotification(
                    context = context,
                    title = "🚨 Monthly Budget Exceeded!",
                    contentSummary = "Spent $currSymbol${String.format("%.2f", totalExpense)} of $currSymbol${String.format("%.2f", budgetLimit)} limit (${String.format("%.0f", percentage)}%)",
                    expandedDetails = "📊 Total Monthly Spending: ${String.format("%.0f", percentage)}%\n" +
                            "• Spent: $currSymbol${String.format("%.2f", totalExpense)}\n" +
                            "• Limit: $currSymbol${String.format("%.2f", budgetLimit)}\n" +
                            "• Exceeded by: $currSymbol${String.format("%.2f", overage)}",
                    progressPercent = 100,
                    isCritical = true,
                    notificationId = 1001,
                    iconName = "wallet"
                )
            } else if (percentage >= 80.0) {
                sendBudgetAlertNotification(
                    context = context,
                    title = "⚠️ Monthly Budget Alert (${String.format("%.0f", percentage)}% Spent)",
                    contentSummary = "Spent $currSymbol${String.format("%.2f", totalExpense)} of $currSymbol${String.format("%.2f", budgetLimit)} budget",
                    expandedDetails = "📊 Monthly Spending Approaching Limit:\n" +
                            "• Spent: $currSymbol${String.format("%.2f", totalExpense)} / $currSymbol${String.format("%.2f", budgetLimit)} (${String.format("%.0f", percentage)}%)\n" +
                            "• Remaining: $currSymbol${String.format("%.2f", remaining)} left for this month",
                    progressPercent = percentage.toInt(),
                    isCritical = false,
                    notificationId = 1001,
                    iconName = "wallet"
                )
            }
        }

        // ==========================================
        // 2. Check Category Budget
        // ==========================================
        if (categoryId != null) {
            val catBudget = budgetRepository.getCategoryBudget(categoryId, month, year).first()
            if (catBudget != null && catBudget.amount > 0.0) {
                val catExpenses = monthlyTransactions
                    .filter { it.transaction.type == "EXPENSE" && it.transaction.categoryId == categoryId }
                    .sumOf { it.transaction.convertedAmount }

                val catLimit = catBudget.amount
                val catPercent = (catExpenses / catLimit) * 100.0
                val catRemaining = max(0.0, catLimit - catExpenses)
                val category = categoryRepository.getCategoryById(categoryId)
                val catName = category?.name ?: "Category"
                val catIcon = category?.icon ?: "more_horiz"

                if (catExpenses >= catLimit) {
                    val catOverage = catExpenses - catLimit
                    sendBudgetAlertNotification(
                        context = context,
                        title = "🚨 Category Limit Exceeded: $catName",
                        contentSummary = "Spent $currSymbol${String.format("%.2f", catExpenses)} of $currSymbol${String.format("%.2f", catLimit)} limit (${String.format("%.0f", catPercent)}%)",
                        expandedDetails = "🏷️ Category Budget ($catName):\n" +
                                "• Spent: $currSymbol${String.format("%.2f", catExpenses)}\n" +
                                "• Limit: $currSymbol${String.format("%.2f", catLimit)}\n" +
                                "• Exceeded by: $currSymbol${String.format("%.2f", catOverage)}",
                        progressPercent = 100,
                        isCritical = true,
                        notificationId = (2000 + categoryId).toInt(),
                        iconName = catIcon
                    )
                } else if (catPercent >= 80.0) {
                    sendBudgetAlertNotification(
                        context = context,
                        title = "⚠️ Category Warning: $catName (${String.format("%.0f", catPercent)}%)",
                        contentSummary = "Spent $currSymbol${String.format("%.2f", catExpenses)} of $currSymbol${String.format("%.2f", catLimit)} budget",
                        expandedDetails = "🏷️ Category Spending Alert ($catName):\n" +
                                "• Spent: $currSymbol${String.format("%.2f", catExpenses)} / $currSymbol${String.format("%.2f", catLimit)} (${String.format("%.0f", catPercent)}%)\n" +
                                "• Remaining: $currSymbol${String.format("%.2f", catRemaining)}",
                        progressPercent = catPercent.toInt(),
                        isCritical = false,
                        notificationId = (2000 + categoryId).toInt(),
                        iconName = catIcon
                    )
                }
            }
        }
    }
}
