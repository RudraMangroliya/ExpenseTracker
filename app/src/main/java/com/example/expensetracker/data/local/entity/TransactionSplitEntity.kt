package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an individual participant's share in a split expense transaction.
 */
@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["phoneNumber"])
    ]
)
data class TransactionSplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val name: String,              // e.g., "Alex", "You"
    val phoneNumber: String = "",  // e.g., "+91 98765 43210"
    val amount: Double,            // Individual share amount
    val isCurrentUser: Boolean = false, // true for "You"
    val isPaid: Boolean = false,   // Whether they have settled up with the user
    val paidDate: Long? = null     // Timestamp when settled
)
