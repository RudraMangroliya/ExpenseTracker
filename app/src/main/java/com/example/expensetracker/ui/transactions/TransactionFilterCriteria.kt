package com.example.expensetracker.ui.transactions

import java.util.Calendar

enum class DateRangePreset(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom")
}

enum class TransactionSortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

data class TransactionFilterCriteria(
    val searchQuery: String = "",
    val type: String = "ALL", // "ALL", "EXPENSE", "INCOME"
    val categoryId: Long? = null,
    val paymentMethod: String = "ALL", // "ALL", "Cash", "Credit Card", "Debit Card", "Bank Transfer", "Other"
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val datePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val isSplitOnly: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (type != "ALL") count++
            if (isSplitOnly) count++
            if (categoryId != null && categoryId > 0) count++
            if (paymentMethod != "ALL") count++
            if (minAmount != null && minAmount > 0.0) count++
            if (maxAmount != null && maxAmount > 0.0) count++
            if (datePreset != DateRangePreset.ALL_TIME) count++
            if (sortOrder != TransactionSortOrder.DATE_DESC) count++
            return count
        }

    val isFiltered: Boolean get() = activeFilterCount > 0 || searchQuery.isNotBlank()

    companion object {
        fun computeDateRange(
            preset: DateRangePreset,
            customStart: Long? = null,
            customEnd: Long? = null
        ): Pair<Long?, Long?> {
            val calendar = Calendar.getInstance()
            return when (preset) {
                DateRangePreset.ALL_TIME -> Pair(null, null)
                DateRangePreset.TODAY -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val end = calendar.timeInMillis
                    Pair(start, end)
                }
                DateRangePreset.THIS_WEEK -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val end = calendar.timeInMillis
                    Pair(start, end)
                }
                DateRangePreset.THIS_MONTH -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val end = calendar.timeInMillis
                    Pair(start, end)
                }
                DateRangePreset.LAST_MONTH -> {
                    calendar.add(Calendar.MONTH, -1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val end = calendar.timeInMillis
                    Pair(start, end)
                }
                DateRangePreset.CUSTOM -> {
                    val start = customStart?.let { s ->
                        Calendar.getInstance().apply {
                            timeInMillis = s
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val end = customEnd?.let { e ->
                        Calendar.getInstance().apply {
                            timeInMillis = e
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                    }
                    Pair(start, end)
                }
            }
        }
    }
}
