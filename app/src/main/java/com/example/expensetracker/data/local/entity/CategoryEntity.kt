package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val icon: String, // Icon identifier name
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
