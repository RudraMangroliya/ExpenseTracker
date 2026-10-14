package com.example.expensetracker

import com.example.expensetracker.data.repository.CurrencyConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun getFallbackRate_sameCurrency_returnsOne() {
        val rate = CurrencyConverter.getFallbackRate("USD", "USD")
        assertEquals(1.0, rate, 0.0001)
    }

    @Test
    fun getFallbackRate_usdToEur_returnsCorrectRatio() {
        val rate = CurrencyConverter.getFallbackRate("USD", "EUR")
        assertEquals(0.8944, rate, 0.0001)
    }

    @Test
    fun getFallbackRate_eurToUsd_returnsInverseRatio() {
        val rate = CurrencyConverter.getFallbackRate("EUR", "USD")
        val expected = 1.0 / 0.8944
        assertEquals(expected, rate, 0.001)
    }

    @Test
    fun getFallbackRate_usdToInr_returnsAccurateRate() {
        val rate = CurrencyConverter.getFallbackRate("USD", "INR")
        assertEquals(95.78, rate, 0.0001)
    }

    @Test
    fun convertAmount_multipliesAmountAndRateWithExactRounding() {
        val converted = CurrencyConverter.convertAmount(100.0, 0.8944)
        assertEquals(89.44, converted, 0.0001)
    }

    @Test
    fun convertAmount_eliminatesFloatingPointDriftOnCents() {
        // 123.45 * 95.78 raw double is 11824.041000000002
        val converted = CurrencyConverter.convertAmount(123.45, 95.78)
        assertEquals(11824.04, converted, 0.0001)
    }

    @Test
    fun roundToTwoDecimals_roundsProperly() {
        assertEquals(12.35, CurrencyConverter.roundToTwoDecimals(12.346), 0.0001)
        assertEquals(12.34, CurrencyConverter.roundToTwoDecimals(12.342), 0.0001)
    }
}
