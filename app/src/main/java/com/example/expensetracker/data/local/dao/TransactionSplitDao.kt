package com.example.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.local.entity.TransactionSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionSplitDao {
    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId ORDER BY isCurrentUser DESC, id ASC")
    fun getSplitsByTransactionId(transactionId: Long): Flow<List<TransactionSplitEntity>>

    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId ORDER BY isCurrentUser DESC, id ASC")
    suspend fun getSplitsByTransactionIdSync(transactionId: Long): List<TransactionSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplitEntity>)

    @Update
    suspend fun updateSplit(split: TransactionSplitEntity)

    @Delete
    suspend fun deleteSplit(split: TransactionSplitEntity)

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun deleteSplitsByTransactionId(transactionId: Long)

    @Query("UPDATE transaction_splits SET isPaid = :isPaid, paidDate = :paidDate WHERE id = :splitId")
    suspend fun updatePaidStatus(splitId: Long, isPaid: Boolean, paidDate: Long?)
}
