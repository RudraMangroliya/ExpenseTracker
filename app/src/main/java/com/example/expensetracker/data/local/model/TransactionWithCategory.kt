package com.example.expensetracker.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.data.local.entity.TransactionSplitEntity

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val splits: List<TransactionSplitEntity> = emptyList()
)
