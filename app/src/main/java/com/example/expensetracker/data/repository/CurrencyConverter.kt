package com.example.expensetracker.data.repository

import java.math.BigDecimal
import java.math.RoundingMode

object CurrencyConverter {

    val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "JPY", "CAD", "AUD")

    // Fallback static exchange rates relative to USD (1 USD = X target, based on official ECB baseline)
    val FALLBACK_RATES_FROM_USD = mapOf(
        "USD" to 1.0,
        "EUR" to 0.8944,
        "GBP" to 0.7556,
        "JPY" to 158.60,
        "CAD" to 1.4262,
        "AUD" to 1.4339,
        "INR" to 95.78
    )

    fun getFallbackRate(fromCurrency: String, toCurrency: String): Double {
        val from = fromCurrency.uppercase().trim()
        val to = toCurrency.uppercase().trim()
        if (from == to) return 1.0

        val fromToUsdRate = FALLBACK_RATES_FROM_USD[from]
        val toToUsdRate = FALLBACK_RATES_FROM_USD[to]

        if (fromToUsdRate != null && toToUsdRate != null && fromToUsdRate > 0.0) {
            return BigDecimal.valueOf(toToUsdRate)
                .divide(BigDecimal.valueOf(fromToUsdRate), 8, RoundingMode.HALF_EVEN)
                .toDouble()
        }
        return 1.0
    }

    /**
     * Converts [amount] using [rate] with exact decimal arithmetic (Banker's rounding, half-even)
     * to eliminate floating-point drift on tenths and hundredths.
     */
    fun convertAmount(amount: Double, rate: Double): Double {
        if (!amount.isFinite() || amount.isNaN()) return 0.0
        if (!rate.isFinite() || rate.isNaN() || rate <= 0.0) return roundToTwoDecimals(amount)

        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(rate))
            .setScale(2, RoundingMode.HALF_EVEN)
            .toDouble()
    }

    fun roundToTwoDecimals(value: Double): Double {
        if (!value.isFinite() || value.isNaN()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_EVEN)
            .toDouble()
    }
}
