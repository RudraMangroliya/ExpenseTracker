package com.example.expensetracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.dao.BudgetDao
import com.example.expensetracker.data.local.dao.CategoryDao
import com.example.expensetracker.data.local.dao.TransactionDao
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseCrudTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var budgetDao: BudgetDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
        transactionDao = database.transactionDao()
        budgetDao = database.budgetDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndQueryCategory() = runBlocking {
        val category = CategoryEntity(name = "Test Food", type = "EXPENSE", icon = "fastfood")
        val catId = categoryDao.insertCategory(category)
        assertTrue(catId > 0)

        val fetched = categoryDao.getCategoryById(catId)
        assertNotNull(fetched)
        assertEquals("Test Food", fetched?.name)
    }

    @Test
    fun transactionCrudOperations() = runBlocking {
        // 1. Insert Category
        val catId = categoryDao.insertCategory(CategoryEntity(name = "Groceries", type = "EXPENSE", icon = "shopping_bag"))

        // 2. Insert Transaction
        val tx = TransactionEntity(
            type = "EXPENSE",
            amount = 50.0,
            currency = "USD",
            convertedAmount = 50.0,
            categoryId = catId,
            date = System.currentTimeMillis(),
            note = "Grocery run",
            paymentMethod = "Credit Card"
        )
        val txId = transactionDao.insertTransaction(tx)
        assertTrue(txId > 0)

        // 3. Query Transactions
        val allTx = transactionDao.getAllTransactions().first()
        assertEquals(1, allTx.size)
        assertEquals("Grocery run", allTx[0].transaction.note)
        assertEquals("Groceries", allTx[0].category?.name)

        // 4. Update Transaction
        val updatedTx = tx.copy(id = txId, amount = 75.0, convertedAmount = 75.0, note = "Updated grocery run")
        transactionDao.updateTransaction(updatedTx)

        val updatedAllTx = transactionDao.getAllTransactions().first()
        assertEquals(75.0, updatedAllTx[0].transaction.amount, 0.001)
        assertEquals("Updated grocery run", updatedAllTx[0].transaction.note)

        // 5. Delete Transaction
        transactionDao.deleteTransactionById(txId)
        val afterDeleteTx = transactionDao.getAllTransactions().first()
        assertTrue(afterDeleteTx.isEmpty())
    }

    @Test
    fun budgetCrudOperations() = runBlocking {
        val budget = BudgetEntity(
            categoryId = null,
            amount = 1500.0,
            currency = "USD",
            month = 5,
            year = 2025
        )
        val budgetId = budgetDao.insertOrUpdateBudget(budget)
        assertTrue(budgetId > 0)

        val fetchedBudget = budgetDao.getOverallBudget(5, 2025).first()
        assertNotNull(fetchedBudget)
        assertEquals(1500.0, fetchedBudget?.amount ?: 0.0, 0.001)
    }
}
