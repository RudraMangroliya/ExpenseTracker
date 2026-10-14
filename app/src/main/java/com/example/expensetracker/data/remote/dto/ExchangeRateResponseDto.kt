package com.example.expensetracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponseDto(
    @SerializedName("amount")
    val amount: Double = 1.0,
    @SerializedName("base")
    val base: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("rates")
    val rates: Map<String, Double>
)
