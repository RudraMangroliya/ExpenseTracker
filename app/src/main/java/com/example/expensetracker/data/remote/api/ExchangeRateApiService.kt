package com.example.expensetracker.data.remote.api

import com.example.expensetracker.data.remote.dto.ExchangeRateResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApiService {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") baseCurrency: String
    ): Response<ExchangeRateResponseDto>
}
