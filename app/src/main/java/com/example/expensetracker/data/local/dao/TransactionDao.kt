package com.example.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.data.local.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionWithCategory?>

    @Transaction
    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, id DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT SUM(convertedAmount) FROM transactions WHERE type = 'INCOME' AND date >= :startDate AND date <= :endDate")
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(convertedAmount) FROM transactions WHERE type = 'EXPENSE' AND date >= :startDate AND date <= :endDate")
    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(convertedAmount) FROM transactions WHERE type = 'INCOME'")
    fun getOverallTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(convertedAmount) FROM transactions WHERE type = 'EXPENSE'")
    fun getOverallTotalExpense(): Flow<Double?>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionEntities(): List<TransactionEntity>

    @Update
    suspend fun updateTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}
