package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.dao.BudgetDao
import com.example.expensetracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val budgetDao: BudgetDao
) {
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(month, year)
    }

    fun getOverallBudget(month: Int, year: Int): Flow<BudgetEntity?> {
        return budgetDao.getOverallBudget(month, year)
    }

    fun getCategoryBudget(categoryId: Long, month: Int, year: Int): Flow<BudgetEntity?> {
        return budgetDao.getCategoryBudget(categoryId, month, year)
    }

    suspend fun setBudget(budget: BudgetEntity): Long {
        val existing = if (budget.categoryId == null) {
            budgetDao.getOverallBudgetDirect(budget.month, budget.year)
        } else {
            budgetDao.getCategoryBudgetDirect(budget.categoryId, budget.month, budget.year)
        }
        val entityToSave = if (existing != null) {
            budget.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = System.currentTimeMillis())
        } else {
            budget.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        }
        return budgetDao.insertOrUpdateBudget(entityToSave)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun clearAllBudgets() {
        budgetDao.clearAllBudgets()
    }
}
