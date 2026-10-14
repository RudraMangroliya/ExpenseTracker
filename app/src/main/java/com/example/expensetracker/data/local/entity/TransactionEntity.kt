package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["type", "date"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val currency: String, // e.g., "USD", "EUR"
    val convertedAmount: Double, // Converted to user base currency
    val categoryId: Long,
    val date: Long, // Epoch timestamp (ms)
    val note: String = "",
    val paymentMethod: String = "Cash", // Cash, Credit Card, Debit Card, Bank Transfer, Other
    val isSplit: Boolean = false, // Whether this was a split bill
    val originalTotalAmount: Double = 0.0, // Full bill amount before split
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
