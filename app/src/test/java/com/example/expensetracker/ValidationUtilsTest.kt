package com.example.expensetracker

import com.example.expensetracker.utils.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validateAmount_emptyString_returnsError() {
        val error = ValidationUtils.validateAmount("")
        assertNotNull(error)
        assertEquals("Amount is required", error)
    }

    @Test
    fun validateAmount_nonNumericString_returnsError() {
        val error = ValidationUtils.validateAmount("abc")
        assertNotNull(error)
        assertEquals("Enter a valid numeric amount", error)
    }

    @Test
    fun validateAmount_zeroOrNegative_returnsError() {
        val errorZero = ValidationUtils.validateAmount("0")
        assertNotNull(errorZero)
        assertEquals("Amount must be greater than zero", errorZero)

        val errorNegative = ValidationUtils.validateAmount("-15.5")
        assertNotNull(errorNegative)
        assertEquals("Amount must be greater than zero", errorNegative)
    }

    @Test
    fun validateAmount_validPositive_returnsNull() {
        val error = ValidationUtils.validateAmount("100.50")
        assertNull(error)
    }

    @Test
    fun validateCategory_nullOrZero_returnsError() {
        assertNotNull(ValidationUtils.validateCategory(null))
        assertNotNull(ValidationUtils.validateCategory(0L))
        assertNull(ValidationUtils.validateCategory(1L))
    }

    @Test
    fun validateNote_maxLength_returnsErrorIfExceeded() {
        val longNote = "a".repeat(251)
        assertNotNull(ValidationUtils.validateNote(longNote))

        val validNote = "a".repeat(250)
        assertNull(ValidationUtils.validateNote(validNote))
    }

    @Test
    fun formatDate_and_parseDate_correctlyTransforms() {
        val now = 1700000000000L
        val formatted = ValidationUtils.formatDate(now, "yyyy-MM-dd")
        val parsed = ValidationUtils.parseDate(formatted, "yyyy-MM-dd")
        assertNotNull(parsed)
    }
}
