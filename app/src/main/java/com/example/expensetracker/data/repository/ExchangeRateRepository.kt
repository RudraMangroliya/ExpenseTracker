package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.dao.ExchangeRateDao
import com.example.expensetracker.data.local.entity.ExchangeRateEntity
import com.example.expensetracker.data.remote.api.ExchangeRateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

data class RateResult(
    val rate: Double,
    val isCached: Boolean,
    val fetchedAt: Long?
)

class ExchangeRateRepository(
    private val apiServiceProvider: () -> ExchangeRateApiService,
    private val exchangeRateDao: ExchangeRateDao
) {

    constructor(
        apiService: ExchangeRateApiService,
        exchangeRateDao: ExchangeRateDao
    ) : this({ apiService }, exchangeRateDao)

    private val apiService: ExchangeRateApiService by lazy { apiServiceProvider() }

    private val memoryRateCache = java.util.concurrent.ConcurrentHashMap<String, RateResult>()

    suspend fun getRate(fromCurrency: String, toCurrency: String): RateResult = withContext(Dispatchers.IO) {
        val from = fromCurrency.uppercase().trim()
        val to = toCurrency.uppercase().trim()

        if (from == to) {
            return@withContext RateResult(rate = 1.0, isCached = false, fetchedAt = System.currentTimeMillis())
        }

        val cacheKey = "$from->$to"
        val inMemory = memoryRateCache[cacheKey]
        if (inMemory != null) {
            return@withContext inMemory
        }

        // 1. Try direct cached rate from Room DB (Instant local disk lookup)
        val directCached = exchangeRateDao.getRate(from, to)
        if (directCached != null && directCached.rate > 0.0 && directCached.rate.isFinite()) {
            val result = RateResult(
                rate = directCached.rate,
                isCached = true,
                fetchedAt = directCached.fetchedAt
            )
            memoryRateCache[cacheKey] = result
            return@withContext result
        }

        // 2. Try inverse cached rate from Room DB (1.0 / rate(to -> from))
        val inverseCached = exchangeRateDao.getRate(to, from)
        if (inverseCached != null && inverseCached.rate > 0.0 && inverseCached.rate.isFinite()) {
            val invRate = BigDecimal.ONE.divide(BigDecimal.valueOf(inverseCached.rate), 8, RoundingMode.HALF_EVEN).toDouble()
            val result = RateResult(
                rate = invRate,
                isCached = true,
                fetchedAt = inverseCached.fetchedAt
            )
            memoryRateCache[cacheKey] = result
            return@withContext result
        }

        // 3. Triangulation via common anchor bases from Room DB (USD, EUR, INR, GBP)
        val triangulatedRate = tryTriangulateFromCache(from, to)
        if (triangulatedRate != null) {
            memoryRateCache[cacheKey] = triangulatedRate
            return@withContext triangulatedRate
        }

        // 4. If absent in cache, attempt Live API
        val now = System.currentTimeMillis()
        try {
            val response = apiService.getLatestRates(from)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val targetRate = dto.rates[to]

                // Save full set of rates + bidirectional inverses + cross-rates
                saveRatesAndCrossRates(dto.base.uppercase(), dto.rates, now)

                if (targetRate != null && targetRate > 0.0 && targetRate.isFinite()) {
                    val result = RateResult(rate = targetRate, isCached = false, fetchedAt = now)
                    memoryRateCache[cacheKey] = result
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            // Network failed or offline - fall through to fallback static conversion
        }

        // 5. High-precision fallback static conversion
        val fallback = CurrencyConverter.getFallbackRate(from, to)
        val result = RateResult(
            rate = fallback,
            isCached = true,
            fetchedAt = null
        )
        memoryRateCache[cacheKey] = result
        return@withContext result
    }

    suspend fun refreshRates(baseCurrency: String): Boolean = withContext(Dispatchers.IO) {
        val base = baseCurrency.uppercase().trim()
        try {
            val response = apiService.getLatestRates(base)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val now = System.currentTimeMillis()
                saveRatesAndCrossRates(dto.base.uppercase(), dto.rates, now)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun saveRatesAndCrossRates(base: String, rates: Map<String, Double>, timestamp: Long) {
        val entities = mutableListOf<ExchangeRateEntity>()

        // Normalize rates map with uppercase keys and valid positive values
        val validRates = mutableMapOf<String, Double>()
        validRates[base] = 1.0
        rates.forEach { (code, rateVal) ->
            if (rateVal > 0.0 && rateVal.isFinite()) {
                validRates[code.uppercase()] = rateVal
            }
        }

        // Build all direct and inverse rates with base
        validRates.forEach { (target, rateVal) ->
            if (target != base) {
                // Base -> Target
                entities.add(
                    ExchangeRateEntity(
                        baseCurrency = base,
                        targetCurrency = target,
                        rate = rateVal,
                        fetchedAt = timestamp
                    )
                )
                // Target -> Base (Inverse)
                val invRate = BigDecimal.ONE.divide(BigDecimal.valueOf(rateVal), 8, RoundingMode.HALF_EVEN).toDouble()
                entities.add(
                    ExchangeRateEntity(
                        baseCurrency = target,
                        targetCurrency = base,
                        rate = invRate,
                        fetchedAt = timestamp
                    )
                )
            }
        }

        // Build cross-rates for supported currencies present in response
        val supportedKeys = validRates.keys.filter { it in CurrencyConverter.SUPPORTED_CURRENCIES || it == base }
        for (c1 in supportedKeys) {
            val r1 = validRates[c1] ?: continue
            for (c2 in supportedKeys) {
                if (c1 == c2) continue
                val r2 = validRates[c2] ?: continue
                if (r1 > 0.0 && r2 > 0.0) {
                    val crossRate = BigDecimal.valueOf(r2)
                        .divide(BigDecimal.valueOf(r1), 8, RoundingMode.HALF_EVEN)
                        .toDouble()
                    entities.add(
                        ExchangeRateEntity(
                            baseCurrency = c1,
                            targetCurrency = c2,
                            rate = crossRate,
                            fetchedAt = timestamp
                        )
                    )
                }
            }
        }

        if (entities.isNotEmpty()) {
            exchangeRateDao.insertRates(entities)
        }
    }

    private suspend fun tryTriangulateFromCache(from: String, to: String): RateResult? {
        val anchorBases = listOf("USD", "EUR", "INR", "GBP")
        for (anchor in anchorBases) {
            if (anchor == from || anchor == to) continue
            val rateAnchorToFrom = exchangeRateDao.getRate(anchor, from) ?: continue
            val rateAnchorToTo = exchangeRateDao.getRate(anchor, to) ?: continue

            if (rateAnchorToFrom.rate > 0.0 && rateAnchorToTo.rate > 0.0) {
                val triangulated = BigDecimal.valueOf(rateAnchorToTo.rate)
                    .divide(BigDecimal.valueOf(rateAnchorToFrom.rate), 8, RoundingMode.HALF_EVEN)
                    .toDouble()
                val latestTimestamp = maxOf(rateAnchorToFrom.fetchedAt, rateAnchorToTo.fetchedAt)
                return RateResult(
                    rate = triangulated,
                    isCached = true,
                    fetchedAt = latestTimestamp
                )
            }
        }
        return null
    }

    suspend fun clearCachedRates() = withContext(Dispatchers.IO) {
        try {
            memoryRateCache.clear()
            exchangeRateDao.clearRates()
        } catch (_: Exception) {}
    }
}
