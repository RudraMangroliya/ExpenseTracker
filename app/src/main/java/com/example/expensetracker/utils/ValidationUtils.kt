package com.example.expensetracker.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object ValidationUtils {

    // ThreadLocal caches to avoid heavy SimpleDateFormat & NumberFormat allocations during fast scrolling
    private val dateFormatCache = ConcurrentHashMap<String, ThreadLocal<SimpleDateFormat>>()
    private val currencyFormatCache = ConcurrentHashMap<String, ThreadLocal<NumberFormat>>()

    private fun getSimpleDateFormat(pattern: String): SimpleDateFormat {
        return dateFormatCache.getOrPut(pattern) {
            ThreadLocal.withInitial {
                SimpleDateFormat(pattern, Locale.getDefault())
            }
        }.get() ?: SimpleDateFormat(pattern, Locale.getDefault())
    }

    private fun getCurrencyFormat(currencyCode: String): NumberFormat {
        return currencyFormatCache.getOrPut(currencyCode) {
            ThreadLocal.withInitial {
                val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
                try {
                    format.currency = Currency.getInstance(currencyCode)
                    if (format is java.text.DecimalFormat) {
                        val dfs = format.decimalFormatSymbols
                        dfs.currencySymbol = getCurrencyDetail(currencyCode).symbol
                        format.decimalFormatSymbols = dfs
                    }
                } catch (_: Exception) {}
                format
            }
        }.get() ?: NumberFormat.getCurrencyInstance(Locale.getDefault())
    }

    fun validateAmount(amountStr: String): String? {
        val trimmed = amountStr.trim()
        if (trimmed.isEmpty()) return "Amount is required"
        val value = trimmed.toDoubleOrNull() ?: return "Enter a valid numeric amount"
        if (value.isNaN() || !value.isFinite()) return "Enter a valid numeric amount"
        if (value <= 0.0) return "Amount must be greater than zero"
        if (value > 1_000_000_000_000.0) return "Amount exceeds maximum supported limit"
        return null
    }

    fun validateCategory(categoryId: Long?): String? {
        if (categoryId == null || categoryId <= 0) return "Category selection is required"
        return null
    }

    fun validateNote(note: String): String? {
        if (note.length > 250) return "Note cannot exceed 250 characters"
        return null
    }

    fun formatCurrency(amount: Double, currencyCode: String): String {
        val safeAmount = if (amount.isNaN() || !amount.isFinite()) 0.0 else amount
        return try {
            getCurrencyFormat(currencyCode).format(safeAmount)
        } catch (e: Exception) {
            String.format(Locale.getDefault(), "%s %.2f", currencyCode, safeAmount)
        }
    }

    fun formatDate(timestampMs: Long, pattern: String = "MMM dd, yyyy"): String {
        return getSimpleDateFormat(pattern).format(Date(timestampMs))
    }

    fun parseDate(dateStr: String, pattern: String = "yyyy-MM-dd"): Long? {
        return try {
            getSimpleDateFormat(pattern).parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    data class CurrencyDetail(
        val code: String,
        val symbol: String,
        val name: String
    )

    fun getCurrencyDetail(code: String): CurrencyDetail {
        return when (code.uppercase()) {
            "INR" -> CurrencyDetail("INR", "₹", "Indian Rupee")
            "USD" -> CurrencyDetail("USD", "$", "US Dollar")
            "EUR" -> CurrencyDetail("EUR", "€", "Euro")
            "GBP" -> CurrencyDetail("GBP", "£", "British Pound")
            "JPY" -> CurrencyDetail("JPY", "¥", "Japanese Yen")
            "CAD" -> CurrencyDetail("CAD", "C$", "Canadian Dollar")
            "AUD" -> CurrencyDetail("AUD", "A$", "Australian Dollar")
            else -> CurrencyDetail(code, code, code)
        }
    }

    fun formatLastSyncTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Never synced"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L} min ago"
            diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
            else -> getSimpleDateFormat("MMM dd, hh:mm a").format(Date(timestamp))
        }
    }
}
