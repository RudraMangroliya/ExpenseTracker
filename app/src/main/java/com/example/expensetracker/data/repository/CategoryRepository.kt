package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.dao.CategoryDao
import com.example.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class CategoryRepository(
    private val categoryDao: CategoryDao
) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .onStart {
            ensureDefaultCategories()
        }

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
            .onStart {
                ensureDefaultCategories()
            }
    }

    private suspend fun ensureDefaultCategories() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(AppDatabase.DEFAULT_CATEGORIES)
        }
    }

    suspend fun getCategoryById(id: Long): CategoryEntity? {
        ensureDefaultCategories()
        return categoryDao.getCategoryById(id)
    }

    suspend fun addCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }
}
