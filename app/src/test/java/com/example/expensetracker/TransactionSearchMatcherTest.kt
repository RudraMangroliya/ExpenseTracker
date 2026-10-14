package com.example.expensetracker

import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.data.local.model.TransactionWithCategory
import com.example.expensetracker.utils.TransactionSearchMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionSearchMatcherTest {

    private fun createSampleTransaction(
        year: Int = 2026,
        month: Int = Calendar.OCTOBER, // Month is 0-indexed in Calendar
        day: Int = 13,
        amount: Double = 1056.0,
        currency: String = "INR",
        categoryName: String = "Food & Dining",
        note: String = "Dinner with team at Italian Bistro",
        paymentMethod: String = "Credit Card",
        type: String = "EXPENSE"
    ): TransactionWithCategory {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val tx = TransactionEntity(
            id = 1L,
            amount = amount,
            currency = currency,
            convertedAmount = amount,
            type = type,
            categoryId = 10L,
            date = cal.timeInMillis,
            note = note,
            paymentMethod = paymentMethod,
            createdAt = cal.timeInMillis
        )

        val cat = CategoryEntity(
            id = 10L,
            name = categoryName,
            type = type,
            icon = "restaurant",
            isDefault = true
        )

        return TransactionWithCategory(transaction = tx, category = cat)
    }

    @Test
    fun testSearchByMonth() {
        val item = createSampleTransaction()

        // Full month name
        assertTrue(TransactionSearchMatcher.matches(item, "October"))
        assertTrue(TransactionSearchMatcher.matches(item, "october"))
        assertTrue(TransactionSearchMatcher.matches(item, "OCTOBER"))

        // Short month name
        assertTrue(TransactionSearchMatcher.matches(item, "Oct"))
        assertTrue(TransactionSearchMatcher.matches(item, "oct"))

        // Numeric month
        assertTrue(TransactionSearchMatcher.matches(item, "10"))

        // Other months should not match
        assertFalse(TransactionSearchMatcher.matches(item, "November"))
        assertFalse(TransactionSearchMatcher.matches(item, "January"))
    }

    @Test
    fun testSearchByYear() {
        val item = createSampleTransaction(year = 2026)

        assertTrue(TransactionSearchMatcher.matches(item, "2026"))
        assertFalse(TransactionSearchMatcher.matches(item, "2024"))
        assertFalse(TransactionSearchMatcher.matches(item, "2025"))
    }

    @Test
    fun testSearchByDayAndDate() {
        val item = createSampleTransaction(day = 13)

        // Day of month
        assertTrue(TransactionSearchMatcher.matches(item, "13"))
        assertTrue(TransactionSearchMatcher.matches(item, "13th"))

        // Full date formats
        assertTrue(TransactionSearchMatcher.matches(item, "13/10/2026"))
        assertTrue(TransactionSearchMatcher.matches(item, "13-10-2026"))
        assertTrue(TransactionSearchMatcher.matches(item, "2026-10-13"))
        assertTrue(TransactionSearchMatcher.matches(item, "13 Oct"))
        assertTrue(TransactionSearchMatcher.matches(item, "Oct 13"))
        assertTrue(TransactionSearchMatcher.matches(item, "October 13"))
        assertTrue(TransactionSearchMatcher.matches(item, "13/10"))

        // Non-matching day
        assertFalse(TransactionSearchMatcher.matches(item, "29th"))
    }

    @Test
    fun testSearchByMonthAndYearCombined() {
        val item = createSampleTransaction(year = 2026, month = Calendar.OCTOBER)

        assertTrue(TransactionSearchMatcher.matches(item, "October 2026"))
        assertTrue(TransactionSearchMatcher.matches(item, "Oct 2026"))
        assertTrue(TransactionSearchMatcher.matches(item, "2026 Oct"))

        assertFalse(TransactionSearchMatcher.matches(item, "October 2025"))
        assertFalse(TransactionSearchMatcher.matches(item, "November 2026"))
    }

    @Test
    fun testSearchByCategoryNoteAndPayment() {
        val item = createSampleTransaction()

        // Category
        assertTrue(TransactionSearchMatcher.matches(item, "Food"))
        assertTrue(TransactionSearchMatcher.matches(item, "Dining"))
        assertTrue(TransactionSearchMatcher.matches(item, "food & dining"))

        // Note
        assertTrue(TransactionSearchMatcher.matches(item, "Dinner"))
        assertTrue(TransactionSearchMatcher.matches(item, "Italian"))
        assertTrue(TransactionSearchMatcher.matches(item, "Bistro"))

        // Payment Method
        assertTrue(TransactionSearchMatcher.matches(item, "Credit Card"))
        assertTrue(TransactionSearchMatcher.matches(item, "Credit"))
        assertTrue(TransactionSearchMatcher.matches(item, "Card"))

        // Transaction Type
        assertTrue(TransactionSearchMatcher.matches(item, "Expense"))
        assertFalse(TransactionSearchMatcher.matches(item, "Income"))
    }

    @Test
    fun testSearchByAmountAndCurrency() {
        val item = createSampleTransaction(amount = 1056.0, currency = "INR")

        assertTrue(TransactionSearchMatcher.matches(item, "1056"))
        assertTrue(TransactionSearchMatcher.matches(item, "1056.00"))
        assertTrue(TransactionSearchMatcher.matches(item, "1,056"))
        assertTrue(TransactionSearchMatcher.matches(item, "INR"))
        assertTrue(TransactionSearchMatcher.matches(item, "₹"))
        assertTrue(TransactionSearchMatcher.matches(item, "Rupee"))

        assertFalse(TransactionSearchMatcher.matches(item, "9999"))
        assertFalse(TransactionSearchMatcher.matches(item, "USD"))
    }

    @Test
    fun testMultiTokenSearchEverything() {
        val item = createSampleTransaction(
            year = 2026,
            month = Calendar.OCTOBER,
            day = 13,
            amount = 1056.0,
            categoryName = "Food & Dining",
            note = "Dinner team",
            paymentMethod = "Credit Card",
            type = "EXPENSE"
        )

        // Multiple keywords matching different fields:
        assertTrue(TransactionSearchMatcher.matches(item, "Food Oct 2026"))
        assertTrue(TransactionSearchMatcher.matches(item, "Dinner 1056"))
        assertTrue(TransactionSearchMatcher.matches(item, "Dinner Credit 2026"))
        assertTrue(TransactionSearchMatcher.matches(item, "Card 13th October"))
        assertTrue(TransactionSearchMatcher.matches(item, "Expense Food 1056"))

        // If one token does not match, entire query fails
        assertFalse(TransactionSearchMatcher.matches(item, "Food Oct 2025")) // wrong year
        assertFalse(TransactionSearchMatcher.matches(item, "Dinner Cash")) // wrong payment
        assertFalse(TransactionSearchMatcher.matches(item, "Groceries 2026")) // wrong category
    }

    @Test
    fun testQuerySpecifiesDateDetection() {
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("2026"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("October"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("oct"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("13th"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("13/10/2026"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("today"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("yesterday"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("Friday"))
        assertTrue(TransactionSearchMatcher.querySpecifiesDate("Food Oct 2026"))

        assertFalse(TransactionSearchMatcher.querySpecifiesDate("Food"))
        assertFalse(TransactionSearchMatcher.querySpecifiesDate("Dinner"))
        assertFalse(TransactionSearchMatcher.querySpecifiesDate("Amazon 500"))
    }
}
