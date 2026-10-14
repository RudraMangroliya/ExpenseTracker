package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exchange_rates",
    indices = [
        Index(value = ["baseCurrency", "targetCurrency"], unique = true)
    ]
)
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val fetchedAt: Long = System.currentTimeMillis()
)
