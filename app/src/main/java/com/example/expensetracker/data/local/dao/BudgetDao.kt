package com.example.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expensetracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY updatedAt DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year ORDER BY updatedAt DESC")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId IS NULL ORDER BY updatedAt DESC, id DESC LIMIT 1")
    fun getOverallBudget(month: Int, year: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId = :categoryId ORDER BY updatedAt DESC, id DESC LIMIT 1")
    fun getCategoryBudget(categoryId: Long, month: Int, year: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId IS NULL ORDER BY updatedAt DESC, id DESC LIMIT 1")
    suspend fun getOverallBudgetDirect(month: Int, year: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId = :categoryId ORDER BY updatedAt DESC, id DESC LIMIT 1")
    suspend fun getCategoryBudgetDirect(categoryId: Long, month: Int, year: Int): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()
}
