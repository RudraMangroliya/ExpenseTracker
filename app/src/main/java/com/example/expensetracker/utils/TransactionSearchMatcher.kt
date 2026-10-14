package com.example.expensetracker.utils

import com.example.expensetracker.data.local.model.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Universal High-Performance Transaction Search Engine.
 *
 * Supports comprehensive keyword & multi-token search across EVERYTHING:
 * 1. Date (day number e.g. "13", ordinal e.g. "13th", day of week e.g. "Tuesday"/"Tue", "today", "yesterday")
 * 2. Month (full name e.g. "October", short name e.g. "Oct", numeric e.g. "10", "10th month")
 * 3. Year (e.g. "2024", "2025", "2026", "'26")
 * 4. Combined Date Formats (e.g. "13/10/2026", "2026-10-13", "13-10-2026", "13.10.2026", "13 Oct", "Oct 13", "Oct 2026", "October 2026")
 * 5. Category (e.g. "Food", "Groceries", "Shopping", "Transport", "Salary", etc.)
 * 6. Note / Description / Remarks (e.g. "Dinner", "Coffee", "Amazon", etc.)
 * 7. Payment Method (e.g. "Cash", "Credit Card", "Debit Card", "UPI", "Bank Transfer", etc.)
 * 8. Transaction Type (e.g. "Expense", "Income")
 * 9. Currency (code e.g. "INR", "USD", symbol e.g. "₹", "$", full name e.g. "Rupee", "Dollar")
 * 10. Amount (raw, integer, 2-decimals, formatted with commas e.g. "1,056", "₹1,056.00", and converted amounts)
 *
 * Supports multi-token composite search:
 * Every space-separated keyword in the query must match at least one attribute of the transaction
 * (e.g. "Food Oct 2026", "Cash 500", "Dinner 13th", "Expense October").
 */
object TransactionSearchMatcher {

    private val DATE_MENTION_REGEX = Regex(
        "(?i)\\b(january|february|march|april|may|june|july|august|september|october|november|december|" +
                "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec|" +
                "monday|tuesday|wednesday|thursday|friday|saturday|sunday|" +
                "mon|tue|wed|thu|fri|sat|sun|" +
                "today|yesterday)\\b|" +
                "\\b(19|20)\\d{2}\\b|" +
                "\\b\\d{1,2}(st|nd|rd|th)\\b|" +
                "\\b\\d{1,2}[/.-]\\d{1,2}([/.-]\\d{2,4})?\\b"
    )

    /**
     * Checks if the user's search query explicitly mentions date, month, or year.
     * When true, this allows date search queries (e.g. "2025", "January") to bypass
     * narrow date range filter presets (like THIS_MONTH).
     */
    fun querySpecifiesDate(query: String): Boolean {
        if (query.isBlank()) return false
        return DATE_MENTION_REGEX.containsMatchIn(query.trim())
    }

    /**
     * Evaluates whether a transaction matches the given search query across all dimensions.
     */
    fun matches(item: TransactionWithCategory, query: String): Boolean {
        if (query.isBlank()) return true

        val cleanQuery = query.trim().lowercase()
        val tokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true

        val tx = item.transaction
        val cat = item.category

        // Calendar breakdowns
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val year = cal.get(Calendar.YEAR).toString()
        val shortYear = if (year.length >= 2) year.takeLast(2) else ""
        val monthInt = cal.get(Calendar.MONTH) + 1
        val monthNum = monthInt.toString()
        val monthNumPadded = String.format(Locale.ROOT, "%02d", monthInt)
        val dayInt = cal.get(Calendar.DAY_OF_MONTH)
        val dayNum = dayInt.toString()
        val dayNumPadded = String.format(Locale.ROOT, "%02d", dayInt)

        val dayOrdinal = when (dayInt) {
            1, 21, 31 -> "${dayInt}st"
            2, 22 -> "${dayInt}nd"
            3, 23 -> "${dayInt}rd"
            else -> "${dayInt}th"
        }

        val monthLong = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)?.lowercase().orEmpty()
        val monthShort = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)?.lowercase().orEmpty()
        val dayOfWeekLong = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH)?.lowercase().orEmpty()
        val dayOfWeekShort = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH)?.lowercase().orEmpty()

        // Relative days
        val nowCal = Calendar.getInstance()
        val isToday = nowCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = nowCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

        // Standard numeric date strings (DMY, YMD, MDY)
        val dmySlash = "$dayNumPadded/$monthNumPadded/$year"
        val dmyDash = "$dayNumPadded-$monthNumPadded-$year"
        val dmyDot = "$dayNumPadded.$monthNumPadded.$year"
        val ymdDash = "$year-$monthNumPadded-$dayNumPadded"
        val ymdSlash = "$year/$monthNumPadded/$dayNumPadded"
        val mdySlash = "$monthNumPadded/$dayNumPadded/$year"
        val mdyDash = "$monthNumPadded-$dayNumPadded-$year"
        val dmyShortSlash = "$dayNumPadded/$monthNumPadded/$shortYear"
        val dmyShortDash = "$dayNumPadded-$monthNumPadded-$shortYear"
        val dmSlash = "$dayNumPadded/$monthNumPadded"
        val dmDash = "$dayNumPadded-$monthNumPadded"

        // Formatted date texts (e.g., "13 Oct", "Oct 13", "October 2026")
        val dateDayMonth = "$dayNum $monthShort"
        val dateDayMonthPadded = "$dayNumPadded $monthShort"
        val dateMonthDay = "$monthShort $dayNum"
        val dateMonthDayPadded = "$monthShort $dayNumPadded"
        val dateDayMonthLong = "$dayNum $monthLong"
        val dateMonthDayLong = "$monthLong $dayNum"
        val dateMonthYear = "$monthLong $year"
        val dateShortMonthYear = "$monthShort $year"
        val dateFullFormatted = "$dayNum $monthShort $year"
        val dateFullLong = "$dayNum $monthLong $year"

        // Currency details
        val currencyDetail = ValidationUtils.getCurrencyDetail(tx.currency)
        val currencyCode = tx.currency.lowercase()
        val currencySymbol = currencyDetail.symbol.lowercase()
        val currencyName = currencyDetail.name.lowercase()

        // Amount representations
        val amountRaw = tx.amount.toString()
        val amountInt = tx.amount.toLong().toString()
        val amount2Dec = String.format(Locale.ROOT, "%.2f", tx.amount)
        val formattedAmount = ValidationUtils.formatCurrency(tx.amount, tx.currency).lowercase()
        val formattedAmountClean = formattedAmount.replace("[^0-9.,]".toRegex(), "").trim()

        val convertedAmountRaw = if (tx.convertedAmount > 0.0) tx.convertedAmount.toString() else ""
        val convertedAmountInt = if (tx.convertedAmount > 0.0) tx.convertedAmount.toLong().toString() else ""
        val convertedAmount2Dec = if (tx.convertedAmount > 0.0) String.format(Locale.ROOT, "%.2f", tx.convertedAmount) else ""
        val formattedConverted = if (tx.convertedAmount > 0.0) {
            ValidationUtils.formatCurrency(tx.convertedAmount, tx.currency).lowercase()
        } else ""
        val formattedConvertedClean = formattedConverted.replace("[^0-9.,]".toRegex(), "").trim()

        // Category, Note, Payment, Type
        val note = tx.note.lowercase()
        val categoryName = cat?.name?.lowercase().orEmpty()
        val paymentMethod = tx.paymentMethod.lowercase()
        val type = tx.type.lowercase()

        // Build comprehensive searchable corpus
        val corpus = buildString(capacity = 512) {
            // Note & Category
            append(note).append(' ')
            append(categoryName).append(' ')
            append(paymentMethod).append(' ')
            append(type).append(' ')

            // Split Bill Details & Participants
            if (tx.isSplit) {
                append("split split bill ")
                item.splits.forEach { split ->
                    append(split.name.lowercase()).append(' ')
                    val cleanDigits = split.phoneNumber.filter { it.isDigit() }
                    if (cleanDigits.isNotEmpty()) {
                        append(cleanDigits).append(' ')
                        if (cleanDigits.length == 10) {
                            append("91").append(cleanDigits).append(' ')
                        }
                    }
                    append(split.amount.toString()).append(' ')
                    append(if (split.isPaid) "paid settled " else "unpaid pending ")
                }
                if (tx.originalTotalAmount > 0.0) {
                    append(tx.originalTotalAmount.toString()).append(' ')
                    append(ValidationUtils.formatCurrency(tx.originalTotalAmount, tx.currency).lowercase()).append(' ')
                }
            }

            // Currency & Amount
            append(currencyCode).append(' ')
            append(currencySymbol).append(' ')
            append(currencyName).append(' ')
            append(amountRaw).append(' ')
            append(amountInt).append(' ')
            append(amount2Dec).append(' ')
            append(formattedAmount).append(' ')
            append(formattedAmountClean).append(' ')

            if (convertedAmountRaw.isNotEmpty()) {
                append(convertedAmountRaw).append(' ')
                append(convertedAmountInt).append(' ')
                append(convertedAmount2Dec).append(' ')
                append(formattedConverted).append(' ')
                append(formattedConvertedClean).append(' ')
            }

            // Year & Month & Day
            append(year).append(' ')
            append(shortYear).append(' ')
            append('\'').append(shortYear).append(' ')
            append(monthLong).append(' ')
            append(monthShort).append(' ')
            append(monthNum).append(' ')
            append(monthNumPadded).append(' ')
            append(dayNum).append(' ')
            append(dayNumPadded).append(' ')
            append(dayOrdinal.lowercase()).append(' ')
            append(dayOfWeekLong).append(' ')
            append(dayOfWeekShort).append(' ')

            // Formatted Date Strings
            append(dmySlash).append(' ')
            append(dmyDash).append(' ')
            append(dmyDot).append(' ')
            append(ymdDash).append(' ')
            append(ymdSlash).append(' ')
            append(mdySlash).append(' ')
            append(mdyDash).append(' ')
            append(dmyShortSlash).append(' ')
            append(dmyShortDash).append(' ')
            append(dmSlash).append(' ')
            append(dmDash).append(' ')

            // Textual Dates
            append(dateDayMonth).append(' ')
            append(dateDayMonthPadded).append(' ')
            append(dateMonthDay).append(' ')
            append(dateMonthDayPadded).append(' ')
            append(dateDayMonthLong).append(' ')
            append(dateMonthDayLong).append(' ')
            append(dateMonthYear).append(' ')
            append(dateShortMonthYear).append(' ')
            append(dateFullFormatted).append(' ')
            append(dateFullLong).append(' ')

            if (isToday) append("today ")
            if (isYesterday) append("yesterday ")
        }.lowercase()

        // Match all tokens in the query
        for (token in tokens) {
            if (!corpus.contains(token)) {
                return false
            }
        }

        return true
    }
}
