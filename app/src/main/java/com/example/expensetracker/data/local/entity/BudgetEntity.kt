package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["month", "year", "categoryId"]),
        Index(value = ["month", "year"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null, // null means overall monthly budget
    val amount: Double,
    val currency: String,
    val month: Int, // 1 to 12
    val year: Int, // e.g. 2025
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
