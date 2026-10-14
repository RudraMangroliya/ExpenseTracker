package com.example.expensetracker

import com.example.expensetracker.data.repository.CurrencyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ExchangeRatePrecisionTest {

    @Test
    fun largeAmountConversion_exactOnOnesTensHundreds() {
        // Test large transaction $10,000 USD to INR
        val rateUsdToInr = CurrencyConverter.getFallbackRate("USD", "INR")
        assertEquals(95.78, rateUsdToInr, 0.0001)

        val convertedLarge = CurrencyConverter.convertAmount(10_000.0, rateUsdToInr)
        // 10,000 * 95.78 must equal exactly 957,800.00 with zero deviation in ones, tens, hundreds
        assertEquals(957800.00, convertedLarge, 0.0001)

        // Test €5,000 EUR to USD
        val rateEurToUsd = CurrencyConverter.getFallbackRate("EUR", "USD")
        val convertedEur = CurrencyConverter.convertAmount(5000.0, rateEurToUsd)
        val expectedEur = BigDecimal.valueOf(5000.0)
            .multiply(BigDecimal.valueOf(1.0).divide(BigDecimal.valueOf(0.8944), 8, RoundingMode.HALF_EVEN))
            .setScale(2, RoundingMode.HALF_EVEN)
            .toDouble()
        assertEquals(expectedEur, convertedEur, 0.01)
    }

    @Test
    fun fractionalCentConversion_exactOnTenthsAndHundredths() {
        // 123.45 USD at 95.78 INR:
        // Raw double multiplication: 123.45 * 95.78 = 11824.041000000002
        // Banker's rounding should give 11824.04 exactly
        val rate = 95.78
        val converted = CurrencyConverter.convertAmount(123.45, rate)
        assertEquals(11824.04, converted, 0.0001)

        // 99.99 EUR at 107.0885 INR:
        val eurToInr = CurrencyConverter.getFallbackRate("EUR", "INR")
        val convertedEur = CurrencyConverter.convertAmount(99.99, eurToInr)
        // Check that result only has 2 decimal places
        val roundedCheck = CurrencyConverter.roundToTwoDecimals(convertedEur)
        assertEquals(roundedCheck, convertedEur, 0.00001)
    }

    @Test
    fun bidirectionalConsistency_forAllSupportedCurrencies() {
        val currencies = CurrencyConverter.SUPPORTED_CURRENCIES
        for (c1 in currencies) {
            for (c2 in currencies) {
                if (c1 == c2) {
                    assertEquals(1.0, CurrencyConverter.getFallbackRate(c1, c2), 0.0001)
                } else {
                    val forward = CurrencyConverter.getFallbackRate(c1, c2)
                    val reverse = CurrencyConverter.getFallbackRate(c2, c1)
                    val product = forward * reverse
                    // Forward and reverse rates multiplied together should equal 1.0 (within precision tolerance)
                    assertTrue("Expected product of $c1->$c2 ($forward) and $c2->$c1 ($reverse) to equal 1.0, got $product",
                        Math.abs(product - 1.0) < 0.001)
                }
            }
        }
    }
}
